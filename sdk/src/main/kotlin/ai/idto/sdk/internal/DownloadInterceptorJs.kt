package ai.idto.sdk.internal

object DownloadInterceptorJs {

    fun build(): String = """(function installDownloadHook() {
  try {
    if (window.__IDTO_DL_HOOKED__) return;
    window.__IDTO_DL_HOOKED__ = true;

    var MAX_DL_BYTES = 50 * 1024 * 1024;

    function post(type, payload) {
      try {
        if (window.IDtoAndroid) {
          window.IDtoAndroid.postMessage(JSON.stringify({ type: type, payload: payload }));
        }
      } catch (e) {}
    }

    var blobs = {};

    function patchURLClass(URLClass) {
      if (!URLClass || !URLClass.createObjectURL || URLClass.__IDTO_PATCHED__) return;
      URLClass.__IDTO_PATCHED__ = true;
      var origCreate = URLClass.createObjectURL;
      var origRevoke = URLClass.revokeObjectURL;
      URLClass.createObjectURL = function (obj) {
        var url = origCreate.apply(URLClass, arguments);
        try { if (url && typeof Blob !== 'undefined' && obj instanceof Blob) blobs[url] = obj; } catch (e) {}
        return url;
      };
      if (origRevoke) {
        URLClass.revokeObjectURL = function (url) {
          try { setTimeout(function () { try { delete blobs[url]; } catch (e) {} }, 5000); } catch (e) {}
          return origRevoke.apply(URLClass, arguments);
        };
      }
    }
    patchURLClass(window.URL);
    patchURLClass(window.webkitURL);

    function isDownloadAnchor(a) {
      if (!a || a.nodeName !== 'A') return false;
      if (a.hasAttribute && a.hasAttribute('download')) return true;
      var href = String((a.getAttribute && a.getAttribute('href')) || a.href || '');
      return href.indexOf('blob:') === 0 || href.indexOf('data:') === 0;
    }

    function filenameFor(a, href) {
      try {
        var dl = a && a.getAttribute && a.getAttribute('download');
        if (dl) return String(dl);
        if (href && href.indexOf('blob:') !== 0 && href.indexOf('data:') !== 0) {
          var clean = String(href).split('?')[0].split('#')[0];
          var last = clean.substring(clean.lastIndexOf('/') + 1);
          if (last) return decodeURIComponent(last);
        }
      } catch (e) {}
      return 'report.pdf';
    }

    function emit(blob, filename) {
      try {
        if (blob && typeof blob.size === 'number' && blob.size > MAX_DL_BYTES) {
          post('log', 'download skipped: blob too large (' + blob.size + ' bytes)');
          return;
        }
        var reader = new FileReader();
        reader.onload = function () {
          try {
            var s = String(reader.result || '');
            var comma = s.indexOf(',');
            var base64 = comma >= 0 ? s.substring(comma + 1) : s;
            post('download', { filename: filename, mime: blob.type || 'application/octet-stream', base64: base64 });
          } catch (e) {}
        };
        reader.readAsDataURL(blob);
      } catch (e) {}
    }

    function handleDownload(a) {
      try {
        var href = String((a.getAttribute && a.getAttribute('href')) || a.href || '');
        var filename = filenameFor(a, href);
        if (href.indexOf('blob:') === 0 && blobs[href]) {
          emit(blobs[href], filename);
          return;
        }
        if (window.fetch) {
          window.fetch(href).then(function (r) { return r.blob(); }).then(function (b) { emit(b, filename); })['catch'](function (e) {});
        }
      } catch (e) {}
    }

    if (window.HTMLAnchorElement && HTMLAnchorElement.prototype) {
      var origClick = HTMLAnchorElement.prototype.click;
      HTMLAnchorElement.prototype.click = function () {
        try {
          if (isDownloadAnchor(this)) { handleDownload(this); return; }
        } catch (e) {}
        return origClick.apply(this, arguments);
      };
    }

    document.addEventListener('click', function (ev) {
      try {
        var node = ev.target;
        while (node && node.nodeName !== 'A') node = node.parentNode;
        if (isDownloadAnchor(node)) {
          ev.preventDefault();
          ev.stopPropagation();
          handleDownload(node);
        }
      } catch (e) {}
    }, true);
  } catch (e) {}
})();"""
}
