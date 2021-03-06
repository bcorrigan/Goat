import urllib
import urllib2

def get_page(url, params=None, headers=None, max_size=25*1024):
    # TODO This needs to be put into a python goat utility library.
    code = None
    content = None
    resp = None

    if headers is not None:
        req = urllib2.Request(url, headers=headers)
    else:
        req = urllib2.Request(url)

    agent = ("Mozilla/5.0 (X11; U; Linux i686) Gecko/20071127 "
             "Firefox/2.0.0.11")
    req.add_header("User-agent", agent)

    try:
        if params is not None:
            resp = urllib2.urlopen(req, urllib.urlencode(params))
        else:
            resp = urllib2.urlopen(req)
        content = resp.read(max_size)
        code = 200 # TODO this is not strictly true.
    except urllib2.URLError, e:
        code = str(e)

    return (code, content, resp)
