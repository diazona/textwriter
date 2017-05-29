'''A Python interface to TextWriter

Typical usage goes something like this:

    backend = TextwriterBackendInterface()
    cache = ImageCache(backend)
    
    # generate a request to render an image
    request = RenderRequest(font, size, bold, italic, bgcolor, fgcolor, text)
    # have TextWriter generate the image and send a key
    image_key = cache.get_image_key(request)
    # get the image as PNG content given the key
    image = cache.get_image_by_key(image_key)

    # alternatively: to get an image without it being cached
    image = backend.get_image(request)

    # get the font list
    font_list = backend.get_font_list()
    # add a font; status is True if it worked or False if not (e.g. the file didn't exist)
    status = backend.add_font(filename)
'''

import colorsys
import hashlib
import itertools
import operator
import re
import socket
import struct
import unicodedata
from collections import namedtuple
from datetime import datetime, timedelta
from threading import Event, RLock, Thread, Condition

TEXTWRITER_PORT = 47251
DEFAULT_CACHE_TIME = timedelta(days=7)

## --------------------------- color processing ---------------------------

# I keep the list of named colors in a separate file because it's huge and I
# don't want to bog down the editor, plus if it ever changes, it'll be
# easier to automatically regenerate the other file
import colorlist

scheme_bases = ('rgb','gray','hsb','hsl','cmyk')
schemes = tuple(itertools.chain((s, s + 'a') for s in scheme_bases))

rgba_color = re.compile('|'.join('[0-9a-fA-F]{%d}' % n for n in (3,4,6,8,12,16)))
scheme_color = re.compile(r'((?:' + '|'.join(scheme_bases) + ')(a)?)\s*\(\s*(\d+(?:\.\d+)?%?\s*(?:,\s*\d+(?:\.\d+)?%?)*)(?(2)\s*,\s*(\d+(?:\.\d+)?))\s*\)')

def normalize_channel(channel, scale_FF=False):
    '''Scales a percentage or a number from 0-255 into the range 0-1,
    if scale_FF is False, or 0-255, if scale_FF is true.'''
    if channel.endswith('%'):
        if scale_FF:
            return int(round(float(channel[:-1]) * 2.55))
        else:
            return float(channel[:-1]) / 100.0
    else:
        if scale_FF:
            return int(channel)
        else:
            return int(channel) / 255.0
    
def parse_scheme_color(match):
    scheme, _, channels, alpha = match.groups()
    normalize_channel = ColorConverter.normalize_channel
    channels = channels.split(',')
    if scheme.startswith('rgb'):
        if len(channels) != 3:
            raise ValueError('Wrong number of channels in color spec')
        channels = [normalize_channel(c.strip(), True) for c in channels]
    else:
        if scheme.startswith('hsb'):
            convert = colorsys.hsv_to_rgb
            channels_needed = 3
        elif scheme.startswith('hsl'):
            convert = lambda h,s,l: colorsys.hls_to_rgb(h,l,s)
            channels_needed = 3
        elif scheme.startswith('gray'):
            convert = lambda i: (i,i,i)
            channels_needed = 1
        elif scheme.startswith('cmyk'):
            # From ImageMagick 6.6.1 source code, colorspace.c line 1327
            convert = lambda c,m,y,k: [(1 - k)*(1 - i) for i in (c,m,y)]
            channels_needed = 4
        if len(channels) != channels_needed:
            raise ValueError('Wrong number of channels in color spec')
        channels = [int(round(float(n) * 255)) for n in convert(*[normalize_channel(c.strip()) for c in channels])]
    if alpha:
        alpha = int(round(float(alpha) * 255))
    else:
        alpha = 255
    return channels + [alpha]
    
def normalize_rgba_color(match):
    match = match.group()
    l = len(match)
    if l == 3:
        return [int(c*2, 16) for c in match] + ['ff']
    elif l == 4:
        return [int(c*2, 16) for c in match]
    elif l == 6:
        return [int(match[n:n+2], 16) for n in (0,2,4)] + ['ff']
    elif l == 8:
        return [int(match[n:n+2], 16) for n in (0,2,4,6)]
    # the following trim a 48-bit or 64-bit color space to 32 bits by
    # taking only the high-order byte of each channel. If Java supports
    # higher bit depths than 32, this could be changed (but it might require
    # rewriting every method in this class to emit the larger bit depth).
    elif l == 12:
        return [int(match[n:n+2], 16) for n in (0,4,8)] + ['ff']
    elif l == 16:
        return [int(match[n:n+2], 16) for n in (0,4,8,12)]
    
def parse_color(color):
    if color in colorlist.named_colors:
        return tuple_to_hex(colorlist.named_colors[color])
    m = scheme_color.match(color)
    if m:
        return tuple_to_hex(tuple(parse_scheme_color(m)))
    m = rgba_color.match(color)
    if m:
        return tuple_to_hex(tuple(normalize_rgba_color(m)))
    raise ValueError('Invalid color specification: ' + color)

def tuple_to_hex(color):
    return '{:02x}{:02x}{:02x}{:02x}'.format(*color)

def complete_color(color):
    '''Readline-style completion of color names'''
    l = [c for c in colorlist.named_colors if c.startswith(color)]
    l += [s + '(' for s in schemes if s.startswith(color)]
    return l

## --------------------------- font processing ---------------------------

class FontRecord:
    def __init__(self, family):
        self.family = family
    def __cmp__(self, other):
        if isinstance(other, FontRecord):
            return cmp(self.family, other.family)
        else:
            return NotImplemented
    def __str__(self):
        return self.family
    def __iter__(self):
        return (k for k in self.__dict__ if (not k.startswith('_') and k != 'family'))

## --------------------------- communication ---------------------------

class BackendUnavailable(Exception):
    pass

class RenderRequest(namedtuple('RenderRequest', 'name size bold italic bgcolor fgcolor text')):
    def to_bytes(self):
        return (self.name.encode('utf-8') + b'\n' +
                struct.pack('>B??', int(self.size), bool(self.bold), bool(self.italic)) +
                parse_color(self.bgcolor).encode('utf-8') + b'\n' +
                parse_color(self.fgcolor).encode('utf-8') + b'\n' +
                struct.pack('>B', len(self.text.splitlines())) +
                self.text.encode('utf-8') + b'\n')

class FontRecord:
    def __init__(self, family):
        self.family = family
    def __cmp__(self, other):
        if isinstance(other, FontRecord):
            return cmp(self.family, other.family)
        else:
            return NotImplemented
    def __str__(self):
        return self.family
    def __iter__(self):
        return (k for k in self.__dict__ if (not k.startswith('_') and k != 'family'))

class TextwriterBackendInterface:
    '''An interface to Textwriter's Java backend.'''
    RENDER_MODE = b'\x00'
    FONT_LIST_MODE = b'\x01'
    ADD_FONT_MODE = b'\x02'
    SUCCESS = b'\x00'

    def __init__(self, host='localhost', port=TEXTWRITER_PORT, timeout=1.0):
        # Don't connect more than one interface to a given host/port
        # Hopefully the socket connection should fail if you try
        self.socket = socket.socket()
        self.socket.settimeout(timeout)
        try:
            self.socket.connect((host, port))
        except socket.timeout:
            raise BackendUnavailable()
        self.socket_lock = RLock()

    def get_font_list(self):
        '''Return a list of FontRecord objects representing the fonts available'''
        with self.socket_lock:
            try:
                self.socket.sendall(self.FONT_LIST_MODE)
                recv_buffer = self.socket.recv(4096)
                while not recv_buffer.endswith(b'\n\n'):
                    recv_buffer += self.socket.recv(4096)
            except socket.timeout:
                raise BackendUnavailable()
        font_list = []
        for line in recv_buffer.decode('utf-8').strip().splitlines():
            if '=' in line:
                key, value = line.split('=', 1)
                setattr(font, key, value)
            else:
                font = FontRecord(line)
                font_list.append(font)
        return font_list

    def add_font(self, filename):
        '''Ask the Java backend to add a font from a file'''
        with self.socket_lock:
            try:
                self.socket.sendall(self.ADD_FONT_MODE + filename.encode('utf-8') + b'\n')
                recv_buffer = self.socket.recv(1)
            except socket.timeout:
                raise BackendUnavailable()
        return recv_buffer[0] == self.SUCCESS

    def get_image(self, request):
        '''Get an image from the Java backend given a RenderRequest'''
        with self.socket_lock:
            try:
                self.socket.sendall(self.RENDER_MODE + request.to_bytes())
                recv_buffer = self.socket.recv(4096)
                while 4 > len(recv_buffer):
                    recv_buffer += self.socket.recv(4096)
                size = struct.unpack('>I', recv_buffer[:4])[0]
                recv_buffer = recv_buffer[4:]
                while size > len(recv_buffer):
                    recv_buffer += self.socket.recv(4096)
            except socket.timeout:
                raise BackendUnavailable()
        return recv_buffer

class ImageCache:
    '''A cache for images obtained from the backend. The cache stores each image
    under a key unique to the RenderRequest used to create it.
    
    The get_image_key() method takes a RenderRequest and returns the corresponding
    image key. If the RenderRequest exists in the cache, it'll return the key
    from the cache, otherwise it passes the request on to the TextWriter backend
    to have a new image generated, stores the image, then computes and returns
    the key. Then get_image_by_key() takes a key and returns the actual image data.'''

    def __init__(self, backend, cache_time=DEFAULT_CACHE_TIME):
        # cache_time should be a timedelta
        self.backend = backend
        self.cache_time = cache_time
        self.cache_lock = RLock()
        self.cache_condition = Condition(self.cache_lock)
        self.cache = {} # { RenderRequest: (image_key, image, timestamp), ... }
        self.rcache = {} # { image_key: RenderRequest, ... }
        
    def get_image_by_key(self, image_key):
        with self.cache_lock:
            rreq = self.rcache.get(image_key)
            if rreq is None:
                return None
            img_tuple = self.cache.get(rreq)
        assert img_tuple is not None
        return img_tuple[1]

    @staticmethod
    def iterate_key(string):
        return hashlib.md5(string).hexdigest()[:8]

    def get_image_key(self, request):
        with self.cache_lock:
            # see if the image is already in the cache
            img_tuple = self.cache.get(request)
            if img_tuple == None:
                # find a unique alphanumeric key for this image request
                the_bytes = request.to_bytes()
                img_key = self.iterate_key(the_bytes)
                while img_key in self.rcache:
                    img_key = self.iterate_key(img_key + the_bytes)
                # and store it as a placeholder
                self.rcache[img_key] = request
                self.cache[request] = (None, None, None)
                self.cache_lock.release()
                # send the request off for processing
                try:
                    # if this raises BackendUnavailable we just let it propagate
                    img_data = self.backend.get_image(request)
                finally:
                    self.cache_lock.acquire()
                # put the received data in the cache
                self.cache[request] = (img_key, img_data, datetime.now())
                self.cache_condition.notify_all()
                return img_key
            elif img_tuple == (None, None, None):
                # already queued, so just wait for it
                while self.cache[request] == (None, None, None):
                    self.cache_condition.wait()
                return self.cache[request][0]
            else:
                return img_tuple[0]

    def purge_cache(self):
        '''Deletes entries in the cache which are older than the cache time'''
        now = datetime.now()
        to_delete = []
        with self.cache_lock:
            for request, (key, image, timestamp) in self.cache.items():
                if now - timestamp > self.cache_time:
                    keys.append((request, key))
            for request, key in to_delete:
                del self.cache[request]
                del self.rcache[key]
