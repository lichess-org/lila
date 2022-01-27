// https://github.com/ornicar/fingerprintjs2/commit/9f580f7c79e6246928d54718f598d27adb821054
window.fipr = (function () {
  'use strict';
  var l = function (e, t) {
      (e = [e[0] >>> 16, 65535 & e[0], e[1] >>> 16, 65535 & e[1]]),
        (t = [t[0] >>> 16, 65535 & t[0], t[1] >>> 16, 65535 & t[1]]);
      var n = [0, 0, 0, 0];
      return (
        (n[3] += e[3] + t[3]),
        (n[2] += n[3] >>> 16),
        (n[3] &= 65535),
        (n[2] += e[2] + t[2]),
        (n[1] += n[2] >>> 16),
        (n[2] &= 65535),
        (n[1] += e[1] + t[1]),
        (n[0] += n[1] >>> 16),
        (n[1] &= 65535),
        (n[0] += e[0] + t[0]),
        (n[0] &= 65535),
        [(n[0] << 16) | n[1], (n[2] << 16) | n[3]]
      );
    },
    f = function (e, t) {
      (e = [e[0] >>> 16, 65535 & e[0], e[1] >>> 16, 65535 & e[1]]),
        (t = [t[0] >>> 16, 65535 & t[0], t[1] >>> 16, 65535 & t[1]]);
      var n = [0, 0, 0, 0];
      return (
        (n[3] += e[3] * t[3]),
        (n[2] += n[3] >>> 16),
        (n[3] &= 65535),
        (n[2] += e[2] * t[3]),
        (n[1] += n[2] >>> 16),
        (n[2] &= 65535),
        (n[2] += e[3] * t[2]),
        (n[1] += n[2] >>> 16),
        (n[2] &= 65535),
        (n[1] += e[1] * t[3]),
        (n[0] += n[1] >>> 16),
        (n[1] &= 65535),
        (n[1] += e[2] * t[2]),
        (n[0] += n[1] >>> 16),
        (n[1] &= 65535),
        (n[1] += e[3] * t[1]),
        (n[0] += n[1] >>> 16),
        (n[1] &= 65535),
        (n[0] += e[0] * t[3] + e[1] * t[2] + e[2] * t[1] + e[3] * t[0]),
        (n[0] &= 65535),
        [(n[0] << 16) | n[1], (n[2] << 16) | n[3]]
      );
    },
    g = function (e, t) {
      return 32 === (t %= 64)
        ? [e[1], e[0]]
        : t < 32
        ? [(e[0] << t) | (e[1] >>> (32 - t)), (e[1] << t) | (e[0] >>> (32 - t))]
        : ((t -= 32), [(e[1] << t) | (e[0] >>> (32 - t)), (e[0] << t) | (e[1] >>> (32 - t))]);
    },
    h = function (e, t) {
      return 0 === (t %= 64) ? e : t < 32 ? [(e[0] << t) | (e[1] >>> (32 - t)), e[1] << t] : [e[1] << (t - 32), 0];
    },
    m = function (e, t) {
      return [e[0] ^ t[0], e[1] ^ t[1]];
    },
    p = function (e) {
      return (
        (e = m(e, [0, e[0] >>> 1])),
        (e = f(e, [4283543511, 3981806797])),
        (e = m(e, [0, e[0] >>> 1])),
        (e = f(e, [3301882366, 444984403])),
        (e = m(e, [0, e[0] >>> 1]))
      );
    },
    c = {
      audio: { timeout: 1e3, excludeIOS11: !0 },
      screen: { detectScreenOrientation: !0 },
      plugins: { sortPluginsFor: [/palemoon/i], excludeIE: !1 },
      excludes: { pixelRatio: !1 },
      NOT_AVAILABLE: 'not available',
      ERROR: 'error',
      EXCLUDED: 'excluded',
    },
    d = function (e, t) {
      if (Array.prototype.forEach && e.forEach === Array.prototype.forEach) e.forEach(t);
      else if (e.length === +e.length) for (var n = 0, r = e.length; n < r; n++) t(e[n], n, e);
      else for (var a in e) e.hasOwnProperty(a) && t(e[a], a, e);
    },
    a = function (e, r) {
      var a = [];
      return null == e
        ? a
        : Array.prototype.map && e.map === Array.prototype.map
        ? e.map(r)
        : (d(e, function (e, t, n) {
            a.push(r(e, t, n));
          }),
          a);
    },
    n = function (e) {
      var t = [window.screen.width, window.screen.height];
      return e.screen.detectScreenOrientation && t.sort().reverse(), t;
    },
    r = function (e) {
      if (window.screen.availWidth && window.screen.availHeight) {
        var t = [window.screen.availHeight, window.screen.availWidth];
        return e.screen.detectScreenOrientation && t.sort().reverse(), t;
      }
      return e.NOT_AVAILABLE;
    },
    i = function (e) {
      if (null == navigator.plugins) return e.NOT_AVAILABLE;
      for (var t = [], n = 0, r = navigator.plugins.length; n < r; n++)
        navigator.plugins[n] && t.push(navigator.plugins[n]);
      return (
        u(e) &&
          (t = t.sort(function (e, t) {
            return e.name > t.name ? 1 : e.name < t.name ? -1 : 0;
          })),
        a(t, function (e) {
          var t = a(e, function (e) {
            return [e.type, e.suffixes];
          });
          return [e.name, e.description, t];
        })
      );
    },
    o = function (n) {
      var e = [];
      return (
        (Object.getOwnPropertyDescriptor && Object.getOwnPropertyDescriptor(window, 'ActiveXObject')) ||
        'ActiveXObject' in window
          ? (e = a(
              [
                'AcroPDF.PDF',
                'Adodb.Stream',
                'AgControl.AgControl',
                'DevalVRXCtrl.DevalVRXCtrl.1',
                'MacromediaFlashPaper.MacromediaFlashPaper',
                'Msxml2.DOMDocument',
                'Msxml2.XMLHTTP',
                'PDF.PdfCtrl',
                'QuickTime.QuickTime',
                'QuickTimeCheckObject.QuickTimeCheck.1',
                'RealPlayer',
                'RealPlayer.RealPlayer(tm) ActiveX Control (32-bit)',
                'RealVideo.RealVideo(tm) ActiveX Control (32-bit)',
                'Scripting.Dictionary',
                'SWCtl.SWCtl',
                'Shell.UIHelper',
                'ShockwaveFlash.ShockwaveFlash',
                'Skype.Detection',
                'TDCCtl.TDCCtl',
                'WMPlayer.OCX',
                'rmocx.RealPlayer G2 Control',
                'rmocx.RealPlayer G2 Control.1',
              ],
              function (e) {
                try {
                  return new window.ActiveXObject(e), e;
                } catch (t) {
                  return n.ERROR;
                }
              }
            ))
          : e.push(n.NOT_AVAILABLE),
        navigator.plugins && (e = e.concat(i(n))),
        e
      );
    },
    u = function (e) {
      for (var t = !1, n = 0, r = e.plugins.sortPluginsFor.length; n < r; n++) {
        var a = e.plugins.sortPluginsFor[n];
        if (navigator.userAgent.match(a)) {
          t = !0;
          break;
        }
      }
      return t;
    },
    s = function (e) {
      try {
        return !!window.sessionStorage;
      } catch (t) {
        return e.ERROR;
      }
    },
    A = function (e) {
      try {
        return !!window.localStorage;
      } catch (t) {
        return e.ERROR;
      }
    },
    v = function (e) {
      try {
        return !!window.indexedDB;
      } catch (t) {
        return e.ERROR;
      }
    },
    x = function (e) {
      return navigator.hardwareConcurrency ? navigator.hardwareConcurrency : e.NOT_AVAILABLE;
    },
    w = function (e) {
      return navigator.cpuClass || e.NOT_AVAILABLE;
    },
    E = function (e) {
      return navigator.platform ? navigator.platform : e.NOT_AVAILABLE;
    },
    t = function () {
      var e,
        t = 0;
      'undefined' != typeof navigator.maxTouchPoints
        ? (t = navigator.maxTouchPoints)
        : 'undefined' != typeof navigator.msMaxTouchPoints && (t = navigator.msMaxTouchPoints);
      try {
        document.createEvent('TouchEvent'), (e = !0);
      } catch (n) {
        e = !1;
      }
      return [t, e, 'ontouchstart' in window];
    },
    O = function (e) {
      var t = [],
        n = document.createElement('canvas');
      (n.width = 2e3), (n.height = 200), (n.style.display = 'inline');
      var r = n.getContext('2d');
      return (
        r.rect(0, 0, 10, 10),
        r.rect(2, 2, 6, 6),
        t.push('canvas winding:' + (!1 === r.isPointInPath(5, 5, 'evenodd') ? 'yes' : 'no')),
        (r.textBaseline = 'alphabetic'),
        (r.fillStyle = '#f60'),
        r.fillRect(125, 1, 62, 20),
        (r.fillStyle = '#069'),
        e.dontUseFakeFontInCanvas ? (r.font = '11pt Arial') : (r.font = '11pt no-real-font-123'),
        r.fillText('Cwm fjordbank glyphs vext quiz, \ud83d\ude03', 2, 15),
        (r.fillStyle = 'rgba(102, 204, 0, 0.2)'),
        (r.font = '18pt Arial'),
        r.fillText('Cwm fjordbank glyphs vext quiz, \ud83d\ude03', 4, 45),
        (r.globalCompositeOperation = 'multiply'),
        (r.fillStyle = 'rgb(255,0,255)'),
        r.beginPath(),
        r.arc(50, 50, 50, 0, 2 * Math.PI, !0),
        r.closePath(),
        r.fill(),
        (r.fillStyle = 'rgb(0,255,255)'),
        r.beginPath(),
        r.arc(100, 50, 50, 0, 2 * Math.PI, !0),
        r.closePath(),
        r.fill(),
        (r.fillStyle = 'rgb(255,255,0)'),
        r.beginPath(),
        r.arc(75, 100, 50, 0, 2 * Math.PI, !0),
        r.closePath(),
        r.fill(),
        (r.fillStyle = 'rgb(255,0,255)'),
        r.arc(75, 75, 75, 0, 2 * Math.PI, !0),
        r.arc(75, 75, 25, 0, 2 * Math.PI, !0),
        r.fill('evenodd'),
        n.toDataURL && t.push('canvas fp:' + n.toDataURL()),
        t
      );
    },
    y = function () {
      var e = function (e) {
          return (
            o.clearColor(0, 0, 0, 1),
            o.enable(o.DEPTH_TEST),
            o.depthFunc(o.LEQUAL),
            o.clear(o.COLOR_BUFFER_BIT | o.DEPTH_BUFFER_BIT),
            '[' + e[0] + ', ' + e[1] + ']'
          );
        },
        o = I();
      if (!o) return null;
      var c = [],
        t = o.createBuffer();
      o.bindBuffer(o.ARRAY_BUFFER, t);
      var n = new Float32Array([-0.2, -0.9, 0, 0.4, -0.26, 0, 0, 0.732134444, 0]);
      o.bufferData(o.ARRAY_BUFFER, n, o.STATIC_DRAW), (t.itemSize = 3), (t.numItems = 3);
      var r = o.createProgram(),
        a = o.createShader(o.VERTEX_SHADER);
      o.shaderSource(
        a,
        'attribute vec2 attrVertex;varying vec2 varyinTexCoordinate;uniform vec2 uniformOffset;void main(){varyinTexCoordinate=attrVertex+uniformOffset;gl_Position=vec4(attrVertex,0,1);}'
      ),
        o.compileShader(a);
      var i = o.createShader(o.FRAGMENT_SHADER);
      o.shaderSource(
        i,
        'precision mediump float;varying vec2 varyinTexCoordinate;void main() {gl_FragColor=vec4(varyinTexCoordinate,0,1);}'
      ),
        o.compileShader(i),
        o.attachShader(r, a),
        o.attachShader(r, i),
        o.linkProgram(r),
        o.useProgram(r),
        (r.vertexPosAttrib = o.getAttribLocation(r, 'attrVertex')),
        (r.offsetUniform = o.getUniformLocation(r, 'uniformOffset')),
        o.enableVertexAttribArray(r.vertexPosArray),
        o.vertexAttribPointer(r.vertexPosAttrib, t.itemSize, o.FLOAT, !1, 0, 0),
        o.uniform2f(r.offsetUniform, 1, 1),
        o.drawArrays(o.TRIANGLE_STRIP, 0, t.numItems);
      try {
        c.push(o.canvas.toDataURL());
      } catch (s) {}
      c.push('extensions:' + (o.getSupportedExtensions() || []).join(';')),
        c.push('webgl aliased line width range:' + e(o.getParameter(o.ALIASED_LINE_WIDTH_RANGE))),
        c.push('webgl aliased point size range:' + e(o.getParameter(o.ALIASED_POINT_SIZE_RANGE))),
        c.push('webgl alpha bits:' + o.getParameter(o.ALPHA_BITS)),
        c.push('webgl antialiasing:' + (o.getContextAttributes().antialias ? 'yes' : 'no')),
        c.push('webgl blue bits:' + o.getParameter(o.BLUE_BITS)),
        c.push('webgl depth bits:' + o.getParameter(o.DEPTH_BITS)),
        c.push('webgl green bits:' + o.getParameter(o.GREEN_BITS)),
        c.push(
          'webgl max anisotropy:' +
            (function (e) {
              var t =
                e.getExtension('EXT_texture_filter_anisotropic') ||
                e.getExtension('WEBKIT_EXT_texture_filter_anisotropic') ||
                e.getExtension('MOZ_EXT_texture_filter_anisotropic');
              if (t) {
                var n = e.getParameter(t.MAX_TEXTURE_MAX_ANISOTROPY_EXT);
                return 0 === n && (n = 2), n;
              }
              return null;
            })(o)
        ),
        c.push('webgl max combined texture image units:' + o.getParameter(o.MAX_COMBINED_TEXTURE_IMAGE_UNITS)),
        c.push('webgl max cube map texture size:' + o.getParameter(o.MAX_CUBE_MAP_TEXTURE_SIZE)),
        c.push('webgl max fragment uniform vectors:' + o.getParameter(o.MAX_FRAGMENT_UNIFORM_VECTORS)),
        c.push('webgl max render buffer size:' + o.getParameter(o.MAX_RENDERBUFFER_SIZE)),
        c.push('webgl max texture image units:' + o.getParameter(o.MAX_TEXTURE_IMAGE_UNITS)),
        c.push('webgl max texture size:' + o.getParameter(o.MAX_TEXTURE_SIZE)),
        c.push('webgl max varying vectors:' + o.getParameter(o.MAX_VARYING_VECTORS)),
        c.push('webgl max vertex attribs:' + o.getParameter(o.MAX_VERTEX_ATTRIBS)),
        c.push('webgl max vertex texture image units:' + o.getParameter(o.MAX_VERTEX_TEXTURE_IMAGE_UNITS)),
        c.push('webgl max vertex uniform vectors:' + o.getParameter(o.MAX_VERTEX_UNIFORM_VECTORS)),
        c.push('webgl max viewport dims:' + e(o.getParameter(o.MAX_VIEWPORT_DIMS))),
        c.push('webgl red bits:' + o.getParameter(o.RED_BITS)),
        c.push('webgl renderer:' + o.getParameter(o.RENDERER)),
        c.push('webgl shading language version:' + o.getParameter(o.SHADING_LANGUAGE_VERSION)),
        c.push('webgl stencil bits:' + o.getParameter(o.STENCIL_BITS)),
        c.push('webgl vendor:' + o.getParameter(o.VENDOR)),
        c.push('webgl version:' + o.getParameter(o.VERSION));
      try {
        var u = o.getExtension('WEBGL_debug_renderer_info');
        u &&
          (c.push('webgl unmasked vendor:' + o.getParameter(u.UNMASKED_VENDOR_WEBGL)),
          c.push('webgl unmasked renderer:' + o.getParameter(u.UNMASKED_RENDERER_WEBGL)));
      } catch (s) {}
      return (
        o.getShaderPrecisionFormat &&
          d(['FLOAT', 'INT'], function (i) {
            d(['VERTEX', 'FRAGMENT'], function (a) {
              d(['HIGH', 'MEDIUM', 'LOW'], function (r) {
                d(['precision', 'rangeMin', 'rangeMax'], function (e) {
                  var t = o.getShaderPrecisionFormat(o[a + '_SHADER'], o[r + '_' + i])[e];
                  'precision' !== e && (e = 'precision ' + e);
                  var n = [
                    'webgl ',
                    a.toLowerCase(),
                    ' shader ',
                    r.toLowerCase(),
                    ' ',
                    i.toLowerCase(),
                    ' ',
                    e,
                    ':',
                    t,
                  ].join('');
                  c.push(n);
                });
              });
            });
          }),
        M(o),
        c
      );
    },
    S = function () {
      try {
        var e = I(),
          t = e.getExtension('WEBGL_debug_renderer_info'),
          n = e.getParameter(t.UNMASKED_VENDOR_WEBGL) + '~' + e.getParameter(t.UNMASKED_RENDERER_WEBGL);
        return M(e), n;
      } catch (r) {
        return null;
      }
    },
    C = function () {
      var e = document.createElement('div');
      e.innerHTML = '&nbsp;';
      var t = !(e.className = 'adsbox');
      try {
        document.body.appendChild(e),
          (t = 0 === document.getElementsByClassName('adsbox')[0].offsetHeight),
          document.body.removeChild(e);
      } catch (n) {
        t = !1;
      }
      return t;
    },
    T = function () {
      if ('undefined' != typeof navigator.languages)
        try {
          if (navigator.languages[0].substr(0, 2) !== navigator.language.substr(0, 2)) return !0;
        } catch (e) {
          return !0;
        }
      return !1;
    },
    b = function () {
      return window.screen.width < window.screen.availWidth || window.screen.height < window.screen.availHeight;
    },
    _ = function () {
      var e = navigator.userAgent.toLowerCase(),
        t = navigator.oscpu,
        n = navigator.platform.toLowerCase(),
        r =
          0 <= e.indexOf('windows phone')
            ? 'Windows Phone'
            : 0 <= e.indexOf('windows') ||
              0 <= e.indexOf('win16') ||
              0 <= e.indexOf('win32') ||
              0 <= e.indexOf('win64') ||
              0 <= e.indexOf('win95') ||
              0 <= e.indexOf('win98') ||
              0 <= e.indexOf('winnt') ||
              0 <= e.indexOf('wow64')
            ? 'Windows'
            : 0 <= e.indexOf('android')
            ? 'Android'
            : 0 <= e.indexOf('linux') || 0 <= e.indexOf('cros') || 0 <= e.indexOf('x11')
            ? 'Linux'
            : 0 <= e.indexOf('iphone') ||
              0 <= e.indexOf('ipad') ||
              0 <= e.indexOf('ipod') ||
              0 <= e.indexOf('crios') ||
              0 <= e.indexOf('fxios')
            ? 'iOS'
            : 0 <= e.indexOf('macintosh') || 0 <= e.indexOf('mac_powerpc)')
            ? 'Mac'
            : 'Other';
      if (
        ('ontouchstart' in window || 0 < navigator.maxTouchPoints || 0 < navigator.msMaxTouchPoints) &&
        'Windows' !== r &&
        'Windows Phone' !== r &&
        'Android' !== r &&
        'iOS' !== r &&
        'Other' !== r &&
        -1 === e.indexOf('cros')
      )
        return !0;
      if (void 0 !== t) {
        if (0 <= (t = t.toLowerCase()).indexOf('win') && 'Windows' !== r && 'Windows Phone' !== r) return !0;
        if (0 <= t.indexOf('linux') && 'Linux' !== r && 'Android' !== r) return !0;
        if (0 <= t.indexOf('mac') && 'Mac' !== r && 'iOS' !== r) return !0;
        if ((-1 === t.indexOf('win') && -1 === t.indexOf('linux') && -1 === t.indexOf('mac')) != ('Other' === r))
          return !0;
      }
      return (
        (0 <= n.indexOf('win') && 'Windows' !== r && 'Windows Phone' !== r) ||
        ((0 <= n.indexOf('linux') || 0 <= n.indexOf('android') || 0 <= n.indexOf('pike')) &&
          'Linux' !== r &&
          'Android' !== r) ||
        ((0 <= n.indexOf('mac') || 0 <= n.indexOf('ipad') || 0 <= n.indexOf('ipod') || 0 <= n.indexOf('iphone')) &&
          'Mac' !== r &&
          'iOS' !== r) ||
        (!(0 <= n.indexOf('arm') && 'Windows Phone' === r) &&
          !(0 <= n.indexOf('pike') && 0 <= e.indexOf('opera mini')) &&
          ((n.indexOf('win') < 0 &&
            n.indexOf('linux') < 0 &&
            n.indexOf('mac') < 0 &&
            n.indexOf('iphone') < 0 &&
            n.indexOf('ipad') < 0 &&
            n.indexOf('ipod') < 0) !=
            ('Other' === r) ||
            ('undefined' == typeof navigator.plugins && 'Windows' !== r && 'Windows Phone' !== r)))
      );
    },
    P = function () {
      var e,
        t = navigator.userAgent.toLowerCase(),
        n = navigator.productSub;
      if (0 <= t.indexOf('edge/') || 0 <= t.indexOf('iemobile/')) return !1;
      if (0 <= t.indexOf('opera mini')) return !1;
      if (
        ('Chrome' ===
          (e =
            0 <= t.indexOf('firefox/')
              ? 'Firefox'
              : 0 <= t.indexOf('opera/') || 0 <= t.indexOf(' opr/')
              ? 'Opera'
              : 0 <= t.indexOf('chrome/')
              ? 'Chrome'
              : 0 <= t.indexOf('safari/')
              ? 0 <= t.indexOf('android 1.') ||
                0 <= t.indexOf('android 2.') ||
                0 <= t.indexOf('android 3.') ||
                0 <= t.indexOf('android 4.')
                ? 'AOSP'
                : 'Safari'
              : 0 <= t.indexOf('trident/')
              ? 'Internet Explorer'
              : 'Other') ||
          'Safari' === e ||
          'Opera' === e) &&
        '20030107' !== n
      )
        return !0;
      var r,
        a = eval.toString().length;
      if (37 === a && 'Safari' !== e && 'Firefox' !== e && 'Other' !== e) return !0;
      if (39 === a && 'Internet Explorer' !== e && 'Other' !== e) return !0;
      if (33 === a && 'Chrome' !== e && 'AOSP' !== e && 'Opera' !== e && 'Other' !== e) return !0;
      try {
        throw 'a';
      } catch (i) {
        try {
          i.toSource(), (r = !0);
        } catch (o) {
          r = !1;
        }
      }
      return r && 'Firefox' !== e && 'Other' !== e;
    },
    R = function () {
      var e = document.createElement('canvas');
      return !(!e.getContext || !e.getContext('2d'));
    },
    D = function () {
      if (!R()) return !1;
      var e = I(),
        t = !!window.WebGLRenderingContext && !!e;
      return M(e), t;
    },
    L = function () {
      return (
        'Microsoft Internet Explorer' === navigator.appName ||
        !('Netscape' !== navigator.appName || !/Trident/.test(navigator.userAgent))
      );
    },
    I = function () {
      var e = document.createElement('canvas'),
        t = null;
      try {
        t = e.getContext('webgl') || e.getContext('experimental-webgl');
      } catch (n) {}
      return (t = t || null);
    },
    M = function (e) {
      var t = e.getExtension('WEBGL_lose_context');
      null != t && t.loseContext();
    },
    k = [
      {
        key: 'userAgent',
        getData: function (e) {
          e(navigator.userAgent);
        },
      },
      {
        key: 'webdriver',
        getData: function (e, t) {
          e(null == navigator.webdriver ? t.NOT_AVAILABLE : navigator.webdriver);
        },
      },
      {
        key: 'language',
        getData: function (e, t) {
          e(
            navigator.language ||
              navigator.userLanguage ||
              navigator.browserLanguage ||
              navigator.systemLanguage ||
              t.NOT_AVAILABLE
          );
        },
      },
      {
        key: 'colorDepth',
        getData: function (e, t) {
          e(window.screen.colorDepth || t.NOT_AVAILABLE);
        },
      },
      {
        key: 'deviceMemory',
        getData: function (e, t) {
          e(navigator.deviceMemory || t.NOT_AVAILABLE);
        },
      },
      {
        key: 'pixelRatio',
        getData: function (e, t) {
          e(window.devicePixelRatio || t.NOT_AVAILABLE);
        },
      },
      {
        key: 'hardwareConcurrency',
        getData: function (e, t) {
          e(x(t));
        },
      },
      {
        key: 'screenResolution',
        getData: function (e, t) {
          e(n(t));
        },
      },
      {
        key: 'availableScreenResolution',
        getData: function (e, t) {
          e(r(t));
        },
      },
      {
        key: 'timezoneOffset',
        getData: function (e) {
          e(new Date().getTimezoneOffset());
        },
      },
      {
        key: 'timezone',
        getData: function (e, t) {
          window.Intl && window.Intl.DateTimeFormat
            ? e(new window.Intl.DateTimeFormat().resolvedOptions().timeZone)
            : e(t.NOT_AVAILABLE);
        },
      },
      {
        key: 'sessionStorage',
        getData: function (e, t) {
          e(s(t));
        },
      },
      {
        key: 'localStorage',
        getData: function (e, t) {
          e(A(t));
        },
      },
      {
        key: 'indexedDb',
        getData: function (e, t) {
          e(v(t));
        },
      },
      {
        key: 'addBehavior',
        getData: function (e) {
          e(!(!document.body || !document.body.addBehavior));
        },
      },
      {
        key: 'openDatabase',
        getData: function (e) {
          e(!!window.openDatabase);
        },
      },
      {
        key: 'cpuClass',
        getData: function (e, t) {
          e(w(t));
        },
      },
      {
        key: 'platform',
        getData: function (e, t) {
          e(E(t));
        },
      },
      {
        key: 'plugins',
        getData: function (e, t) {
          L() ? (t.plugins.excludeIE ? e(t.EXCLUDED) : e(o(t))) : e(i(t));
        },
      },
      {
        key: 'canvas',
        getData: function (e, t) {
          R() ? e(O(t)) : e(t.NOT_AVAILABLE);
        },
      },
      {
        key: 'webgl',
        getData: function (e, t) {
          D() ? e(y()) : e(t.NOT_AVAILABLE);
        },
      },
      {
        key: 'webglVendorAndRenderer',
        getData: function (e) {
          D() ? e(S()) : e();
        },
      },
      {
        key: 'adBlock',
        getData: function (e) {
          e(C());
        },
      },
      {
        key: 'hasLiedLanguages',
        getData: function (e) {
          e(T());
        },
      },
      {
        key: 'hasLiedResolution',
        getData: function (e) {
          e(b());
        },
      },
      {
        key: 'hasLiedOs',
        getData: function (e) {
          e(_());
        },
      },
      {
        key: 'hasLiedBrowser',
        getData: function (e) {
          e(P());
        },
      },
      {
        key: 'touchSupport',
        getData: function (e) {
          e(t());
        },
      },
      {
        key: 'fonts',
        getData: function (e, t) {
          var d = ['monospace', 'sans-serif', 'serif'],
            l = (l = [
              'Andale Mono',
              'Arial',
              'Arial Black',
              'Arial Hebrew',
              'Arial MT',
              'Arial Narrow',
              'Arial Rounded MT Bold',
              'Arial Unicode MS',
              'Bitstream Vera Sans Mono',
              'Book Antiqua',
              'Bookman Old Style',
              'Calibri',
              'Cambria',
              'Cambria Math',
              'Century',
              'Century Gothic',
              'Century Schoolbook',
              'Comic Sans',
              'Comic Sans MS',
              'Consolas',
              'Courier',
              'Courier New',
              'Geneva',
              'Georgia',
              'Helvetica',
              'Helvetica Neue',
              'Impact',
              'Lucida Bright',
              'Lucida Calligraphy',
              'Lucida Console',
              'Lucida Fax',
              'LUCIDA GRANDE',
              'Lucida Handwriting',
              'Lucida Sans',
              'Lucida Sans Typewriter',
              'Lucida Sans Unicode',
              'Microsoft Sans Serif',
              'Monaco',
              'Monotype Corsiva',
              'MS Gothic',
              'MS Outlook',
              'MS PGothic',
              'MS Reference Sans Serif',
              'MS Sans Serif',
              'MS Serif',
              'MYRIAD',
              'MYRIAD PRO',
              'Palatino',
              'Palatino Linotype',
              'Segoe Print',
              'Segoe Script',
              'Segoe UI',
              'Segoe UI Light',
              'Segoe UI Semibold',
              'Segoe UI Symbol',
              'Tahoma',
              'Times',
              'Times New Roman',
              'Times New Roman PS',
              'Trebuchet MS',
              'Verdana',
              'Wingdings',
              'Wingdings 2',
              'Wingdings 3',
            ]).filter(function (e, t) {
              return l.indexOf(e) === t;
            }),
            n = document.getElementsByTagName('body')[0],
            a = document.createElement('div'),
            f = document.createElement('div'),
            r = {},
            i = {},
            g = function () {
              var e = document.createElement('span');
              return (
                (e.style.position = 'absolute'),
                (e.style.left = '-9999px'),
                (e.style.fontSize = '72px'),
                (e.style.fontStyle = 'normal'),
                (e.style.fontWeight = 'normal'),
                (e.style.letterSpacing = 'normal'),
                (e.style.lineBreak = 'auto'),
                (e.style.lineHeight = 'normal'),
                (e.style.textTransform = 'none'),
                (e.style.textAlign = 'left'),
                (e.style.textDecoration = 'none'),
                (e.style.textShadow = 'none'),
                (e.style.whiteSpace = 'normal'),
                (e.style.wordBreak = 'normal'),
                (e.style.wordSpacing = 'normal'),
                (e.innerHTML = 'mmmmmmmmmmlli'),
                e
              );
            },
            o = function (e) {
              for (var t = !1, n = 0; n < d.length; n++)
                if ((t = e[n].offsetWidth !== r[d[n]] || e[n].offsetHeight !== i[d[n]])) return t;
              return t;
            },
            c = (function () {
              for (var e = [], t = 0, n = d.length; t < n; t++) {
                var r = g();
                (r.style.fontFamily = d[t]), a.appendChild(r), e.push(r);
              }
              return e;
            })();
          n.appendChild(a);
          for (var u = 0, s = d.length; u < s; u++) (r[d[u]] = c[u].offsetWidth), (i[d[u]] = c[u].offsetHeight);
          var h = (function () {
            for (var e, t, n, r = {}, a = 0, i = l.length; a < i; a++) {
              for (var o = [], c = 0, u = d.length; c < u; c++) {
                var s = ((e = l[a]), (t = d[c]), (n = void 0), ((n = g()).style.fontFamily = "'" + e + "'," + t), n);
                f.appendChild(s), o.push(s);
              }
              r[l[a]] = o;
            }
            return r;
          })();
          n.appendChild(f);
          for (var m = [], p = 0, A = l.length; p < A; p++) o(h[l[p]]) && m.push(l[p]);
          n.removeChild(f), n.removeChild(a), e(m);
        },
        pauseBefore: !0,
      },
      {
        key: 'audio',
        getData: function (r, e) {
          var t = e.audio;
          if (t.excludeIOS11 && navigator.userAgent.match(/OS 11.+Version\/11.+Safari/)) return r(e.EXCLUDED);
          var n = window.OfflineAudioContext || window.webkitOfflineAudioContext;
          if (null == n) return r(e.NOT_AVAILABLE);
          var a = new n(1, 44100, 44100),
            i = a.createOscillator();
          (i.type = 'triangle'), i.frequency.setValueAtTime(1e4, a.currentTime);
          var o = a.createDynamicsCompressor();
          d(
            [
              ['threshold', -50],
              ['knee', 40],
              ['ratio', 12],
              ['reduction', -20],
              ['attack', 0],
              ['release', 0.25],
            ],
            function (e) {
              o[e[0]] !== undefined &&
                'function' == typeof o[e[0]].setValueAtTime &&
                o[e[0]].setValueAtTime(e[1], a.currentTime);
            }
          ),
            i.connect(o),
            o.connect(a.destination),
            i.start(0),
            a.startRendering();
          var c = setTimeout(function () {
            return (
              console.warn(
                'Audio fingerprint timed out. Please report bug at https://github.com/Valve/fingerprintjs2 with your user agent: "' +
                  navigator.userAgent +
                  '".'
              ),
              (a.oncomplete = function () {}),
              (a = null),
              r('audioTimeout')
            );
          }, t.timeout);
          a.oncomplete = function (e) {
            var t;
            try {
              clearTimeout(c),
                (t = e.renderedBuffer
                  .getChannelData(0)
                  .slice(4500, 5e3)
                  .reduce(function (e, t) {
                    return e + Math.abs(t);
                  }, 0)
                  .toString()),
                i.disconnect(),
                o.disconnect();
            } catch (n) {
              return void r(n);
            }
            r(t);
          };
        },
      },
    ],
    e = {
      get: function (r) {
        var a = {
            data: [],
            addPreprocessedComponent: function (e, t) {
              a.data.push({ key: e, value: t });
            },
          },
          i = -1,
          o = function (e) {
            if (k.length <= (i += 1)) r(a.data);
            else {
              var t = k[i];
              if (c.excludes[t.key]) o(!1);
              else {
                if (!e && t.pauseBefore)
                  return (
                    --i,
                    void setTimeout(function () {
                      o(!0);
                    }, 1)
                  );
                try {
                  t.getData(function (e) {
                    a.addPreprocessedComponent(t.key, e), o(!1);
                  }, c);
                } catch (n) {
                  a.addPreprocessedComponent(t.key, String(n)), o(!1);
                }
              }
            }
          };
        o(!1);
      },
      x64hash128: function (e, t) {
        t = t || 0;
        for (
          var n = (e = e || '').length % 16,
            r = e.length - n,
            a = [0, t],
            i = [0, t],
            o = [0, 0],
            c = [0, 0],
            u = [2277735313, 289559509],
            s = [1291169091, 658871167],
            d = 0;
          d < r;
          d += 16
        )
          (o = [
            (255 & e.charCodeAt(d + 4)) |
              ((255 & e.charCodeAt(d + 5)) << 8) |
              ((255 & e.charCodeAt(d + 6)) << 16) |
              ((255 & e.charCodeAt(d + 7)) << 24),
            (255 & e.charCodeAt(d)) |
              ((255 & e.charCodeAt(d + 1)) << 8) |
              ((255 & e.charCodeAt(d + 2)) << 16) |
              ((255 & e.charCodeAt(d + 3)) << 24),
          ]),
            (c = [
              (255 & e.charCodeAt(d + 12)) |
                ((255 & e.charCodeAt(d + 13)) << 8) |
                ((255 & e.charCodeAt(d + 14)) << 16) |
                ((255 & e.charCodeAt(d + 15)) << 24),
              (255 & e.charCodeAt(d + 8)) |
                ((255 & e.charCodeAt(d + 9)) << 8) |
                ((255 & e.charCodeAt(d + 10)) << 16) |
                ((255 & e.charCodeAt(d + 11)) << 24),
            ]),
            (o = f(o, u)),
            (o = g(o, 31)),
            (o = f(o, s)),
            (a = m(a, o)),
            (a = g(a, 27)),
            (a = l(a, i)),
            (a = l(f(a, [0, 5]), [0, 1390208809])),
            (c = f(c, s)),
            (c = g(c, 33)),
            (c = f(c, u)),
            (i = m(i, c)),
            (i = g(i, 31)),
            (i = l(i, a)),
            (i = l(f(i, [0, 5]), [0, 944331445]));
        switch (((o = [0, 0]), (c = [0, 0]), n)) {
          case 15:
            c = m(c, h([0, e.charCodeAt(d + 14)], 48));
          case 14:
            c = m(c, h([0, e.charCodeAt(d + 13)], 40));
          case 13:
            c = m(c, h([0, e.charCodeAt(d + 12)], 32));
          case 12:
            c = m(c, h([0, e.charCodeAt(d + 11)], 24));
          case 11:
            c = m(c, h([0, e.charCodeAt(d + 10)], 16));
          case 10:
            c = m(c, h([0, e.charCodeAt(d + 9)], 8));
          case 9:
            (c = m(c, [0, e.charCodeAt(d + 8)])), (c = f(c, s)), (c = g(c, 33)), (c = f(c, u)), (i = m(i, c));
          case 8:
            o = m(o, h([0, e.charCodeAt(d + 7)], 56));
          case 7:
            o = m(o, h([0, e.charCodeAt(d + 6)], 48));
          case 6:
            o = m(o, h([0, e.charCodeAt(d + 5)], 40));
          case 5:
            o = m(o, h([0, e.charCodeAt(d + 4)], 32));
          case 4:
            o = m(o, h([0, e.charCodeAt(d + 3)], 24));
          case 3:
            o = m(o, h([0, e.charCodeAt(d + 2)], 16));
          case 2:
            o = m(o, h([0, e.charCodeAt(d + 1)], 8));
          case 1:
            (o = m(o, [0, e.charCodeAt(d)])), (o = f(o, u)), (o = g(o, 31)), (o = f(o, s)), (a = m(a, o));
        }
        return (
          (a = m(a, [0, e.length])),
          (i = m(i, [0, e.length])),
          (a = l(a, i)),
          (i = l(i, a)),
          (a = p(a)),
          (i = p(i)),
          (a = l(a, i)),
          (i = l(i, a)),
          ('00000000' + (a[0] >>> 0).toString(16)).slice(-8) +
            ('00000000' + (a[1] >>> 0).toString(16)).slice(-8) +
            ('00000000' + (i[0] >>> 0).toString(16)).slice(-8) +
            ('00000000' + (i[1] >>> 0).toString(16)).slice(-8)
        );
      },
    };
  return e;
})();

lichess.load.then(() =>
  setTimeout(() => {
    const t = performance.now(),
      storage = lichess.storage.make('fipr'),
      send = hash => {
        storage.set(hash);
        const $i = $('#signup-fp-input');
        if ($i.length) $i.val(hash);
        else
          fetch('/auth/set-fp/' + hash + '/' + Math.round(performance.now() - t), {
            method: 'post',
            credentials: 'same-origin',
          });
      };
    if (storage.get()) send(storage.get());
    else fipr.get(c => send(fipr.x64hash128(c.map(x => x.value).join(''), 31)));
  }, 1000)
);
