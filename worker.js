var __defProp = Object.defineProperty;
var __name = (target, value) => __defProp(target, "name", { value, configurable: true });
var __require = /* @__PURE__ */ ((x) => typeof require !== "undefined" ? require : typeof Proxy !== "undefined" ? new Proxy(x, {
  get: (a, b) => (typeof require !== "undefined" ? require : a)[b]
}) : x)(function(x) {
  if (typeof require !== "undefined") return require.apply(this, arguments);
  throw Error('Dynamic require of "' + x + '" is not supported');
});

// worker.js
import libDefault from "events";
import libDefault2 from "util/types";
import libDefault3 from "util";
import libDefault4 from "crypto";
import libDefault5 from "dns";
import libDefault6 from "fs";
import libDefault7 from "net";
import libDefault8 from "tls";
import libDefault9 from "path";
import libDefault10 from "stream";
import libDefault11 from "string_decoder";
var __create = Object.create;
var __defProp2 = Object.defineProperty;
var __getOwnPropDesc = Object.getOwnPropertyDescriptor;
var __getOwnPropNames = Object.getOwnPropertyNames;
var __getProtoOf = Object.getPrototypeOf;
var __hasOwnProp = Object.prototype.hasOwnProperty;
var __name2 = /* @__PURE__ */ __name((target, value) => __defProp2(target, "name", { value, configurable: true }), "__name");
var __require2 = /* @__PURE__ */ ((x) => typeof __require !== "undefined" ? __require : typeof Proxy !== "undefined" ? new Proxy(x, {
  get: /* @__PURE__ */ __name((a, b) => (typeof __require !== "undefined" ? __require : a)[b], "get")
}) : x)(function(x) {
  if (typeof __require !== "undefined") return __require.apply(this, arguments);
  throw Error('Dynamic require of "' + x + '" is not supported');
});
var __commonJS = /* @__PURE__ */ __name((cb, mod) => /* @__PURE__ */ __name(function __require22() {
  try {
    return mod || (0, cb[__getOwnPropNames(cb)[0]])((mod = { exports: {} }).exports, mod), mod.exports;
  } catch (e) {
    throw mod = 0, e;
  }
}, "__require2"), "__commonJS");
var __copyProps = /* @__PURE__ */ __name((to, from, except, desc) => {
  if (from && typeof from === "object" || typeof from === "function") {
    for (let key of __getOwnPropNames(from))
      if (!__hasOwnProp.call(to, key) && key !== except)
        __defProp2(to, key, { get: /* @__PURE__ */ __name(() => from[key], "get"), enumerable: !(desc = __getOwnPropDesc(from, key)) || desc.enumerable });
  }
  return to;
}, "__copyProps");
var __toESM = /* @__PURE__ */ __name((mod, isNodeMode, target) => (target = mod != null ? __create(__getProtoOf(mod)) : {}, __copyProps(
  // If the importer is in node compatibility mode or this is not an ESM
  // file that has been converted to a CommonJS file using a Babel-
  // compatible transform (i.e. "__esModule" has not been set), then set
  // "default" to the CommonJS "module.exports" for node compatibility.
  isNodeMode || !mod || !mod.__esModule ? __defProp2(target, "default", { value: mod, enumerable: true }) : target,
  mod
)), "__toESM");
var require_events = __commonJS({
  "node-built-in-modules:events"(exports, module) {
    module.exports = libDefault;
  }
});
var require_postgres_array = __commonJS({
  "node_modules/postgres-array/index.js"(exports) {
    "use strict";
    exports.parse = function(source, transform) {
      return new ArrayParser(source, transform).parse();
    };
    var ArrayParser = class _ArrayParser {
      static {
        __name(this, "_ArrayParser");
      }
      static {
        __name2(this, "ArrayParser");
      }
      constructor(source, transform) {
        this.source = source;
        this.transform = transform || identity;
        this.position = 0;
        this.entries = [];
        this.recorded = [];
        this.dimension = 0;
      }
      isEof() {
        return this.position >= this.source.length;
      }
      nextCharacter() {
        var character = this.source[this.position++];
        if (character === "\\") {
          return {
            value: this.source[this.position++],
            escaped: true
          };
        }
        return {
          value: character,
          escaped: false
        };
      }
      record(character) {
        this.recorded.push(character);
      }
      newEntry(includeEmpty) {
        var entry;
        if (this.recorded.length > 0 || includeEmpty) {
          entry = this.recorded.join("");
          if (entry === "NULL" && !includeEmpty) {
            entry = null;
          }
          if (entry !== null) entry = this.transform(entry);
          this.entries.push(entry);
          this.recorded = [];
        }
      }
      consumeDimensions() {
        if (this.source[0] === "[") {
          while (!this.isEof()) {
            var char = this.nextCharacter();
            if (char.value === "=") break;
          }
        }
      }
      parse(nested) {
        var character, parser, quote;
        this.consumeDimensions();
        while (!this.isEof()) {
          character = this.nextCharacter();
          if (character.value === "{" && !quote) {
            this.dimension++;
            if (this.dimension > 1) {
              parser = new _ArrayParser(this.source.substr(this.position - 1), this.transform);
              this.entries.push(parser.parse(true));
              this.position += parser.position - 2;
            }
          } else if (character.value === "}" && !quote) {
            this.dimension--;
            if (!this.dimension) {
              this.newEntry();
              if (nested) return this.entries;
            }
          } else if (character.value === '"' && !character.escaped) {
            if (quote) this.newEntry(true);
            quote = !quote;
          } else if (character.value === "," && !quote) {
            this.newEntry();
          } else {
            this.record(character.value);
          }
        }
        if (this.dimension !== 0) {
          throw new Error("array dimension not balanced");
        }
        return this.entries;
      }
    };
    function identity(value) {
      return value;
    }
    __name(identity, "identity");
    __name2(identity, "identity");
  }
});
var require_arrayParser = __commonJS({
  "node_modules/pg-types/lib/arrayParser.js"(exports, module) {
    var array = require_postgres_array();
    module.exports = {
      create: /* @__PURE__ */ __name2(function(source, transform) {
        return {
          parse: /* @__PURE__ */ __name2(function() {
            return array.parse(source, transform);
          }, "parse")
        };
      }, "create")
    };
  }
});
var require_postgres_date = __commonJS({
  "node_modules/postgres-date/index.js"(exports, module) {
    "use strict";
    var DATE_TIME = /(\d{1,})-(\d{2})-(\d{2}) (\d{2}):(\d{2}):(\d{2})(\.\d{1,})?.*?( BC)?$/;
    var DATE = /^(\d{1,})-(\d{2})-(\d{2})( BC)?$/;
    var TIME_ZONE = /([Z+-])(\d{2})?:?(\d{2})?:?(\d{2})?/;
    var INFINITY = /^-?infinity$/;
    module.exports = /* @__PURE__ */ __name2(/* @__PURE__ */ __name(function parseDate(isoDate) {
      if (INFINITY.test(isoDate)) {
        return Number(isoDate.replace("i", "I"));
      }
      var matches = DATE_TIME.exec(isoDate);
      if (!matches) {
        return getDate(isoDate) || null;
      }
      var isBC = !!matches[8];
      var year = parseInt(matches[1], 10);
      if (isBC) {
        year = bcYearToNegativeYear(year);
      }
      var month = parseInt(matches[2], 10) - 1;
      var day = matches[3];
      var hour = parseInt(matches[4], 10);
      var minute = parseInt(matches[5], 10);
      var second = parseInt(matches[6], 10);
      var ms = matches[7];
      ms = ms ? 1e3 * parseFloat(ms) : 0;
      var date;
      var offset = timeZoneOffset(isoDate);
      if (offset != null) {
        date = new Date(Date.UTC(year, month, day, hour, minute, second, ms));
        if (is0To99(year)) {
          date.setUTCFullYear(year);
        }
        if (offset !== 0) {
          date.setTime(date.getTime() - offset);
        }
      } else {
        date = new Date(year, month, day, hour, minute, second, ms);
        if (is0To99(year)) {
          date.setFullYear(year);
        }
      }
      return date;
    }, "parseDate"), "parseDate");
    function getDate(isoDate) {
      var matches = DATE.exec(isoDate);
      if (!matches) {
        return;
      }
      var year = parseInt(matches[1], 10);
      var isBC = !!matches[4];
      if (isBC) {
        year = bcYearToNegativeYear(year);
      }
      var month = parseInt(matches[2], 10) - 1;
      var day = matches[3];
      var date = new Date(year, month, day);
      if (is0To99(year)) {
        date.setFullYear(year);
      }
      return date;
    }
    __name(getDate, "getDate");
    __name2(getDate, "getDate");
    function timeZoneOffset(isoDate) {
      if (isoDate.endsWith("+00")) {
        return 0;
      }
      var zone = TIME_ZONE.exec(isoDate.split(" ")[1]);
      if (!zone) return;
      var type = zone[1];
      if (type === "Z") {
        return 0;
      }
      var sign = type === "-" ? -1 : 1;
      var offset = parseInt(zone[2], 10) * 3600 + parseInt(zone[3] || 0, 10) * 60 + parseInt(zone[4] || 0, 10);
      return offset * sign * 1e3;
    }
    __name(timeZoneOffset, "timeZoneOffset");
    __name2(timeZoneOffset, "timeZoneOffset");
    function bcYearToNegativeYear(year) {
      return -(year - 1);
    }
    __name(bcYearToNegativeYear, "bcYearToNegativeYear");
    __name2(bcYearToNegativeYear, "bcYearToNegativeYear");
    function is0To99(num) {
      return num >= 0 && num < 100;
    }
    __name(is0To99, "is0To99");
    __name2(is0To99, "is0To99");
  }
});
var require_mutable = __commonJS({
  "node_modules/xtend/mutable.js"(exports, module) {
    module.exports = extend;
    var hasOwnProperty = Object.prototype.hasOwnProperty;
    function extend(target) {
      for (var i = 1; i < arguments.length; i++) {
        var source = arguments[i];
        for (var key in source) {
          if (hasOwnProperty.call(source, key)) {
            target[key] = source[key];
          }
        }
      }
      return target;
    }
    __name(extend, "extend");
    __name2(extend, "extend");
  }
});
var require_postgres_interval = __commonJS({
  "node_modules/postgres-interval/index.js"(exports, module) {
    "use strict";
    var extend = require_mutable();
    module.exports = PostgresInterval;
    function PostgresInterval(raw) {
      if (!(this instanceof PostgresInterval)) {
        return new PostgresInterval(raw);
      }
      extend(this, parse(raw));
    }
    __name(PostgresInterval, "PostgresInterval");
    __name2(PostgresInterval, "PostgresInterval");
    var properties = ["seconds", "minutes", "hours", "days", "months", "years"];
    PostgresInterval.prototype.toPostgres = function() {
      var filtered = properties.filter(this.hasOwnProperty, this);
      if (this.milliseconds && filtered.indexOf("seconds") < 0) {
        filtered.push("seconds");
      }
      if (filtered.length === 0) return "0";
      return filtered.map(function(property) {
        var value = this[property] || 0;
        if (property === "seconds" && this.milliseconds) {
          value = (value + this.milliseconds / 1e3).toFixed(6).replace(/\.?0+$/, "");
        }
        return value + " " + property;
      }, this).join(" ");
    };
    var propertiesISOEquivalent = {
      years: "Y",
      months: "M",
      days: "D",
      hours: "H",
      minutes: "M",
      seconds: "S"
    };
    var dateProperties = ["years", "months", "days"];
    var timeProperties = ["hours", "minutes", "seconds"];
    PostgresInterval.prototype.toISOString = PostgresInterval.prototype.toISO = function() {
      var datePart = dateProperties.map(buildProperty, this).join("");
      var timePart = timeProperties.map(buildProperty, this).join("");
      return "P" + datePart + "T" + timePart;
      function buildProperty(property) {
        var value = this[property] || 0;
        if (property === "seconds" && this.milliseconds) {
          value = (value + this.milliseconds / 1e3).toFixed(6).replace(/0+$/, "");
        }
        return value + propertiesISOEquivalent[property];
      }
      __name(buildProperty, "buildProperty");
      __name2(buildProperty, "buildProperty");
    };
    var NUMBER = "([+-]?\\d+)";
    var YEAR = NUMBER + "\\s+years?";
    var MONTH = NUMBER + "\\s+mons?";
    var DAY = NUMBER + "\\s+days?";
    var TIME = "([+-])?([\\d]*):(\\d\\d):(\\d\\d)\\.?(\\d{1,6})?";
    var INTERVAL = new RegExp([YEAR, MONTH, DAY, TIME].map(function(regexString) {
      return "(" + regexString + ")?";
    }).join("\\s*"));
    var positions = {
      years: 2,
      months: 4,
      days: 6,
      hours: 9,
      minutes: 10,
      seconds: 11,
      milliseconds: 12
    };
    var negatives = ["hours", "minutes", "seconds", "milliseconds"];
    function parseMilliseconds(fraction) {
      var microseconds = fraction + "000000".slice(fraction.length);
      return parseInt(microseconds, 10) / 1e3;
    }
    __name(parseMilliseconds, "parseMilliseconds");
    __name2(parseMilliseconds, "parseMilliseconds");
    function parse(interval) {
      if (!interval) return {};
      var matches = INTERVAL.exec(interval);
      var isNegative = matches[8] === "-";
      return Object.keys(positions).reduce(function(parsed, property) {
        var position = positions[property];
        var value = matches[position];
        if (!value) return parsed;
        value = property === "milliseconds" ? parseMilliseconds(value) : parseInt(value, 10);
        if (!value) return parsed;
        if (isNegative && ~negatives.indexOf(property)) {
          value *= -1;
        }
        parsed[property] = value;
        return parsed;
      }, {});
    }
    __name(parse, "parse");
    __name2(parse, "parse");
  }
});
var require_postgres_bytea = __commonJS({
  "node_modules/postgres-bytea/index.js"(exports, module) {
    "use strict";
    var bufferFrom = Buffer.from || Buffer;
    module.exports = /* @__PURE__ */ __name2(/* @__PURE__ */ __name(function parseBytea(input) {
      if (/^\\x/.test(input)) {
        return bufferFrom(input.substr(2), "hex");
      }
      var output = "";
      var i = 0;
      while (i < input.length) {
        if (input[i] !== "\\") {
          output += input[i];
          ++i;
        } else {
          if (/[0-7]{3}/.test(input.substr(i + 1, 3))) {
            output += String.fromCharCode(parseInt(input.substr(i + 1, 3), 8));
            i += 4;
          } else {
            var backslashes = 1;
            while (i + backslashes < input.length && input[i + backslashes] === "\\") {
              backslashes++;
            }
            for (var k = 0; k < Math.floor(backslashes / 2); ++k) {
              output += "\\";
            }
            i += Math.floor(backslashes / 2) * 2;
          }
        }
      }
      return bufferFrom(output, "binary");
    }, "parseBytea"), "parseBytea");
  }
});
var require_textParsers = __commonJS({
  "node_modules/pg-types/lib/textParsers.js"(exports, module) {
    var array = require_postgres_array();
    var arrayParser = require_arrayParser();
    var parseDate = require_postgres_date();
    var parseInterval = require_postgres_interval();
    var parseByteA = require_postgres_bytea();
    function allowNull(fn) {
      return /* @__PURE__ */ __name2(/* @__PURE__ */ __name(function nullAllowed(value) {
        if (value === null) return value;
        return fn(value);
      }, "nullAllowed"), "nullAllowed");
    }
    __name(allowNull, "allowNull");
    __name2(allowNull, "allowNull");
    function parseBool(value) {
      if (value === null) return value;
      return value === "TRUE" || value === "t" || value === "true" || value === "y" || value === "yes" || value === "on" || value === "1";
    }
    __name(parseBool, "parseBool");
    __name2(parseBool, "parseBool");
    function parseBoolArray(value) {
      if (!value) return null;
      return array.parse(value, parseBool);
    }
    __name(parseBoolArray, "parseBoolArray");
    __name2(parseBoolArray, "parseBoolArray");
    function parseBaseTenInt(string) {
      return parseInt(string, 10);
    }
    __name(parseBaseTenInt, "parseBaseTenInt");
    __name2(parseBaseTenInt, "parseBaseTenInt");
    function parseIntegerArray(value) {
      if (!value) return null;
      return array.parse(value, allowNull(parseBaseTenInt));
    }
    __name(parseIntegerArray, "parseIntegerArray");
    __name2(parseIntegerArray, "parseIntegerArray");
    function parseBigIntegerArray(value) {
      if (!value) return null;
      return array.parse(value, allowNull(function(entry) {
        return parseBigInteger(entry).trim();
      }));
    }
    __name(parseBigIntegerArray, "parseBigIntegerArray");
    __name2(parseBigIntegerArray, "parseBigIntegerArray");
    var parsePointArray = /* @__PURE__ */ __name2(function(value) {
      if (!value) {
        return null;
      }
      var p = arrayParser.create(value, function(entry) {
        if (entry !== null) {
          entry = parsePoint(entry);
        }
        return entry;
      });
      return p.parse();
    }, "parsePointArray");
    var parseFloatArray = /* @__PURE__ */ __name2(function(value) {
      if (!value) {
        return null;
      }
      var p = arrayParser.create(value, function(entry) {
        if (entry !== null) {
          entry = parseFloat(entry);
        }
        return entry;
      });
      return p.parse();
    }, "parseFloatArray");
    var parseStringArray = /* @__PURE__ */ __name2(function(value) {
      if (!value) {
        return null;
      }
      var p = arrayParser.create(value);
      return p.parse();
    }, "parseStringArray");
    var parseDateArray = /* @__PURE__ */ __name2(function(value) {
      if (!value) {
        return null;
      }
      var p = arrayParser.create(value, function(entry) {
        if (entry !== null) {
          entry = parseDate(entry);
        }
        return entry;
      });
      return p.parse();
    }, "parseDateArray");
    var parseIntervalArray = /* @__PURE__ */ __name2(function(value) {
      if (!value) {
        return null;
      }
      var p = arrayParser.create(value, function(entry) {
        if (entry !== null) {
          entry = parseInterval(entry);
        }
        return entry;
      });
      return p.parse();
    }, "parseIntervalArray");
    var parseByteAArray = /* @__PURE__ */ __name2(function(value) {
      if (!value) {
        return null;
      }
      return array.parse(value, allowNull(parseByteA));
    }, "parseByteAArray");
    var parseInteger = /* @__PURE__ */ __name2(function(value) {
      return parseInt(value, 10);
    }, "parseInteger");
    var parseBigInteger = /* @__PURE__ */ __name2(function(value) {
      var valStr = String(value);
      if (/^\d+$/.test(valStr)) {
        return valStr;
      }
      return value;
    }, "parseBigInteger");
    var parseJsonArray = /* @__PURE__ */ __name2(function(value) {
      if (!value) {
        return null;
      }
      return array.parse(value, allowNull(JSON.parse));
    }, "parseJsonArray");
    var parsePoint = /* @__PURE__ */ __name2(function(value) {
      if (value[0] !== "(") {
        return null;
      }
      value = value.substring(1, value.length - 1).split(",");
      return {
        x: parseFloat(value[0]),
        y: parseFloat(value[1])
      };
    }, "parsePoint");
    var parseCircle = /* @__PURE__ */ __name2(function(value) {
      if (value[0] !== "<" && value[1] !== "(") {
        return null;
      }
      var point = "(";
      var radius = "";
      var pointParsed = false;
      for (var i = 2; i < value.length - 1; i++) {
        if (!pointParsed) {
          point += value[i];
        }
        if (value[i] === ")") {
          pointParsed = true;
          continue;
        } else if (!pointParsed) {
          continue;
        }
        if (value[i] === ",") {
          continue;
        }
        radius += value[i];
      }
      var result = parsePoint(point);
      result.radius = parseFloat(radius);
      return result;
    }, "parseCircle");
    var init = /* @__PURE__ */ __name2(function(register) {
      register(20, parseBigInteger);
      register(21, parseInteger);
      register(23, parseInteger);
      register(26, parseInteger);
      register(700, parseFloat);
      register(701, parseFloat);
      register(16, parseBool);
      register(1082, parseDate);
      register(1114, parseDate);
      register(1184, parseDate);
      register(600, parsePoint);
      register(651, parseStringArray);
      register(718, parseCircle);
      register(1e3, parseBoolArray);
      register(1001, parseByteAArray);
      register(1005, parseIntegerArray);
      register(1007, parseIntegerArray);
      register(1028, parseIntegerArray);
      register(1016, parseBigIntegerArray);
      register(1017, parsePointArray);
      register(1021, parseFloatArray);
      register(1022, parseFloatArray);
      register(1231, parseFloatArray);
      register(1014, parseStringArray);
      register(1015, parseStringArray);
      register(1008, parseStringArray);
      register(1009, parseStringArray);
      register(1040, parseStringArray);
      register(1041, parseStringArray);
      register(1115, parseDateArray);
      register(1182, parseDateArray);
      register(1185, parseDateArray);
      register(1186, parseInterval);
      register(1187, parseIntervalArray);
      register(17, parseByteA);
      register(114, JSON.parse.bind(JSON));
      register(3802, JSON.parse.bind(JSON));
      register(199, parseJsonArray);
      register(3807, parseJsonArray);
      register(3907, parseStringArray);
      register(2951, parseStringArray);
      register(791, parseStringArray);
      register(1183, parseStringArray);
      register(1270, parseStringArray);
    }, "init");
    module.exports = {
      init
    };
  }
});
var require_pg_int8 = __commonJS({
  "node_modules/pg-int8/index.js"(exports, module) {
    "use strict";
    var BASE = 1e6;
    function readInt8(buffer) {
      var high = buffer.readInt32BE(0);
      var low = buffer.readUInt32BE(4);
      var sign = "";
      if (high < 0) {
        high = ~high + (low === 0);
        low = ~low + 1 >>> 0;
        sign = "-";
      }
      var result = "";
      var carry;
      var t;
      var digits;
      var pad;
      var l;
      var i;
      {
        carry = high % BASE;
        high = high / BASE >>> 0;
        t = 4294967296 * carry + low;
        low = t / BASE >>> 0;
        digits = "" + (t - BASE * low);
        if (low === 0 && high === 0) {
          return sign + digits + result;
        }
        pad = "";
        l = 6 - digits.length;
        for (i = 0; i < l; i++) {
          pad += "0";
        }
        result = pad + digits + result;
      }
      {
        carry = high % BASE;
        high = high / BASE >>> 0;
        t = 4294967296 * carry + low;
        low = t / BASE >>> 0;
        digits = "" + (t - BASE * low);
        if (low === 0 && high === 0) {
          return sign + digits + result;
        }
        pad = "";
        l = 6 - digits.length;
        for (i = 0; i < l; i++) {
          pad += "0";
        }
        result = pad + digits + result;
      }
      {
        carry = high % BASE;
        high = high / BASE >>> 0;
        t = 4294967296 * carry + low;
        low = t / BASE >>> 0;
        digits = "" + (t - BASE * low);
        if (low === 0 && high === 0) {
          return sign + digits + result;
        }
        pad = "";
        l = 6 - digits.length;
        for (i = 0; i < l; i++) {
          pad += "0";
        }
        result = pad + digits + result;
      }
      {
        carry = high % BASE;
        t = 4294967296 * carry + low;
        digits = "" + t % BASE;
        return sign + digits + result;
      }
    }
    __name(readInt8, "readInt8");
    __name2(readInt8, "readInt8");
    module.exports = readInt8;
  }
});
var require_binaryParsers = __commonJS({
  "node_modules/pg-types/lib/binaryParsers.js"(exports, module) {
    var parseInt64 = require_pg_int8();
    var parseBits = /* @__PURE__ */ __name2(function(data, bits, offset, invert, callback) {
      offset = offset || 0;
      invert = invert || false;
      callback = callback || function(lastValue, newValue, bits2) {
        return lastValue * Math.pow(2, bits2) + newValue;
      };
      var offsetBytes = offset >> 3;
      var inv = /* @__PURE__ */ __name2(function(value) {
        if (invert) {
          return ~value & 255;
        }
        return value;
      }, "inv");
      var mask = 255;
      var firstBits = 8 - offset % 8;
      if (bits < firstBits) {
        mask = 255 << 8 - bits & 255;
        firstBits = bits;
      }
      if (offset) {
        mask = mask >> offset % 8;
      }
      var result = 0;
      if (offset % 8 + bits >= 8) {
        result = callback(0, inv(data[offsetBytes]) & mask, firstBits);
      }
      var bytes = bits + offset >> 3;
      for (var i = offsetBytes + 1; i < bytes; i++) {
        result = callback(result, inv(data[i]), 8);
      }
      var lastBits = (bits + offset) % 8;
      if (lastBits > 0) {
        result = callback(result, inv(data[bytes]) >> 8 - lastBits, lastBits);
      }
      return result;
    }, "parseBits");
    var parseFloatFromBits = /* @__PURE__ */ __name2(function(data, precisionBits, exponentBits) {
      var bias = Math.pow(2, exponentBits - 1) - 1;
      var sign = parseBits(data, 1);
      var exponent = parseBits(data, exponentBits, 1);
      if (exponent === 0) {
        return 0;
      }
      var precisionBitsCounter = 1;
      var parsePrecisionBits = /* @__PURE__ */ __name2(function(lastValue, newValue, bits) {
        if (lastValue === 0) {
          lastValue = 1;
        }
        for (var i = 1; i <= bits; i++) {
          precisionBitsCounter /= 2;
          if ((newValue & 1 << bits - i) > 0) {
            lastValue += precisionBitsCounter;
          }
        }
        return lastValue;
      }, "parsePrecisionBits");
      var mantissa = parseBits(data, precisionBits, exponentBits + 1, false, parsePrecisionBits);
      if (exponent == Math.pow(2, exponentBits + 1) - 1) {
        if (mantissa === 0) {
          return sign === 0 ? Infinity : -Infinity;
        }
        return NaN;
      }
      return (sign === 0 ? 1 : -1) * Math.pow(2, exponent - bias) * mantissa;
    }, "parseFloatFromBits");
    var parseInt16 = /* @__PURE__ */ __name2(function(value) {
      if (parseBits(value, 1) == 1) {
        return -1 * (parseBits(value, 15, 1, true) + 1);
      }
      return parseBits(value, 15, 1);
    }, "parseInt16");
    var parseInt32 = /* @__PURE__ */ __name2(function(value) {
      if (parseBits(value, 1) == 1) {
        return -1 * (parseBits(value, 31, 1, true) + 1);
      }
      return parseBits(value, 31, 1);
    }, "parseInt32");
    var parseFloat32 = /* @__PURE__ */ __name2(function(value) {
      return parseFloatFromBits(value, 23, 8);
    }, "parseFloat32");
    var parseFloat64 = /* @__PURE__ */ __name2(function(value) {
      return parseFloatFromBits(value, 52, 11);
    }, "parseFloat64");
    var parseNumeric = /* @__PURE__ */ __name2(function(value) {
      var sign = parseBits(value, 16, 32);
      if (sign == 49152) {
        return NaN;
      }
      var weight = Math.pow(1e4, parseBits(value, 16, 16));
      var result = 0;
      var digits = [];
      var ndigits = parseBits(value, 16);
      for (var i = 0; i < ndigits; i++) {
        result += parseBits(value, 16, 64 + 16 * i) * weight;
        weight /= 1e4;
      }
      var scale = Math.pow(10, parseBits(value, 16, 48));
      return (sign === 0 ? 1 : -1) * Math.round(result * scale) / scale;
    }, "parseNumeric");
    var parseDate = /* @__PURE__ */ __name2(function(isUTC, value) {
      var sign = parseBits(value, 1);
      var rawValue = parseBits(value, 63, 1);
      var result = new Date((sign === 0 ? 1 : -1) * rawValue / 1e3 + 9466848e5);
      if (!isUTC) {
        result.setTime(result.getTime() + result.getTimezoneOffset() * 6e4);
      }
      result.usec = rawValue % 1e3;
      result.getMicroSeconds = function() {
        return this.usec;
      };
      result.setMicroSeconds = function(value2) {
        this.usec = value2;
      };
      result.getUTCMicroSeconds = function() {
        return this.usec;
      };
      return result;
    }, "parseDate");
    var parseArray = /* @__PURE__ */ __name2(function(value) {
      var dim = parseBits(value, 32);
      var flags = parseBits(value, 32, 32);
      var elementType = parseBits(value, 32, 64);
      var offset = 96;
      var dims = [];
      for (var i = 0; i < dim; i++) {
        dims[i] = parseBits(value, 32, offset);
        offset += 32;
        offset += 32;
      }
      var parseElement = /* @__PURE__ */ __name2(function(elementType2) {
        var length = parseBits(value, 32, offset);
        offset += 32;
        if (length == 4294967295) {
          return null;
        }
        var result;
        if (elementType2 == 23 || elementType2 == 20) {
          result = parseBits(value, length * 8, offset);
          offset += length * 8;
          return result;
        } else if (elementType2 == 25) {
          result = value.toString(this.encoding, offset >> 3, (offset += length << 3) >> 3);
          return result;
        } else {
          console.log("ERROR: ElementType not implemented: " + elementType2);
        }
      }, "parseElement");
      var parse = /* @__PURE__ */ __name2(function(dimension, elementType2) {
        var array = [];
        var i2;
        if (dimension.length > 1) {
          var count = dimension.shift();
          for (i2 = 0; i2 < count; i2++) {
            array[i2] = parse(dimension, elementType2);
          }
          dimension.unshift(count);
        } else {
          for (i2 = 0; i2 < dimension[0]; i2++) {
            array[i2] = parseElement(elementType2);
          }
        }
        return array;
      }, "parse");
      return parse(dims, elementType);
    }, "parseArray");
    var parseText = /* @__PURE__ */ __name2(function(value) {
      return value.toString("utf8");
    }, "parseText");
    var parseBool = /* @__PURE__ */ __name2(function(value) {
      if (value === null) return null;
      return parseBits(value, 8) > 0;
    }, "parseBool");
    var init = /* @__PURE__ */ __name2(function(register) {
      register(20, parseInt64);
      register(21, parseInt16);
      register(23, parseInt32);
      register(26, parseInt32);
      register(1700, parseNumeric);
      register(700, parseFloat32);
      register(701, parseFloat64);
      register(16, parseBool);
      register(1114, parseDate.bind(null, false));
      register(1184, parseDate.bind(null, true));
      register(1e3, parseArray);
      register(1007, parseArray);
      register(1016, parseArray);
      register(1008, parseArray);
      register(1009, parseArray);
      register(25, parseText);
    }, "init");
    module.exports = {
      init
    };
  }
});
var require_builtins = __commonJS({
  "node_modules/pg-types/lib/builtins.js"(exports, module) {
    module.exports = {
      BOOL: 16,
      BYTEA: 17,
      CHAR: 18,
      INT8: 20,
      INT2: 21,
      INT4: 23,
      REGPROC: 24,
      TEXT: 25,
      OID: 26,
      TID: 27,
      XID: 28,
      CID: 29,
      JSON: 114,
      XML: 142,
      PG_NODE_TREE: 194,
      SMGR: 210,
      PATH: 602,
      POLYGON: 604,
      CIDR: 650,
      FLOAT4: 700,
      FLOAT8: 701,
      ABSTIME: 702,
      RELTIME: 703,
      TINTERVAL: 704,
      CIRCLE: 718,
      MACADDR8: 774,
      MONEY: 790,
      MACADDR: 829,
      INET: 869,
      ACLITEM: 1033,
      BPCHAR: 1042,
      VARCHAR: 1043,
      DATE: 1082,
      TIME: 1083,
      TIMESTAMP: 1114,
      TIMESTAMPTZ: 1184,
      INTERVAL: 1186,
      TIMETZ: 1266,
      BIT: 1560,
      VARBIT: 1562,
      NUMERIC: 1700,
      REFCURSOR: 1790,
      REGPROCEDURE: 2202,
      REGOPER: 2203,
      REGOPERATOR: 2204,
      REGCLASS: 2205,
      REGTYPE: 2206,
      UUID: 2950,
      TXID_SNAPSHOT: 2970,
      PG_LSN: 3220,
      PG_NDISTINCT: 3361,
      PG_DEPENDENCIES: 3402,
      TSVECTOR: 3614,
      TSQUERY: 3615,
      GTSVECTOR: 3642,
      REGCONFIG: 3734,
      REGDICTIONARY: 3769,
      JSONB: 3802,
      REGNAMESPACE: 4089,
      REGROLE: 4096
    };
  }
});
var require_pg_types = __commonJS({
  "node_modules/pg-types/index.js"(exports) {
    var textParsers = require_textParsers();
    var binaryParsers = require_binaryParsers();
    var arrayParser = require_arrayParser();
    var builtinTypes = require_builtins();
    exports.getTypeParser = getTypeParser;
    exports.setTypeParser = setTypeParser;
    exports.arrayParser = arrayParser;
    exports.builtins = builtinTypes;
    var typeParsers = {
      text: {},
      binary: {}
    };
    function noParse(val) {
      return String(val);
    }
    __name(noParse, "noParse");
    __name2(noParse, "noParse");
    function getTypeParser(oid, format) {
      format = format || "text";
      if (!typeParsers[format]) {
        return noParse;
      }
      return typeParsers[format][oid] || noParse;
    }
    __name(getTypeParser, "getTypeParser");
    __name2(getTypeParser, "getTypeParser");
    function setTypeParser(oid, format, parseFn) {
      if (typeof format == "function") {
        parseFn = format;
        format = "text";
      }
      typeParsers[format][oid] = parseFn;
    }
    __name(setTypeParser, "setTypeParser");
    __name2(setTypeParser, "setTypeParser");
    textParsers.init(function(oid, converter) {
      typeParsers.text[oid] = converter;
    });
    binaryParsers.init(function(oid, converter) {
      typeParsers.binary[oid] = converter;
    });
  }
});
var require_defaults = __commonJS({
  "node_modules/pg/lib/defaults.js"(exports, module) {
    "use strict";
    var user;
    try {
      user = process.platform === "win32" ? process.env.USERNAME : process.env.USER;
    } catch {
    }
    module.exports = {
      // database host. defaults to localhost
      host: "localhost",
      // database user's name
      user,
      // name of database to connect
      database: void 0,
      // database user's password
      password: null,
      // a Postgres connection string to be used instead of setting individual connection items
      // NOTE:  Setting this value will cause it to override any other value (such as database or user) defined
      // in the defaults object.
      connectionString: void 0,
      // database port
      port: 5432,
      // number of rows to return at a time from a prepared statement's
      // portal. 0 will return all rows at once
      rows: 0,
      // binary result mode
      binary: false,
      // Connection pool options - see https://github.com/brianc/node-pg-pool
      // number of connections to use in connection pool
      // 0 will disable connection pooling
      max: 10,
      // max milliseconds a client can go unused before it is removed
      // from the pool and destroyed
      idleTimeoutMillis: 3e4,
      client_encoding: "",
      ssl: false,
      // SSL negotiation style: 'postgres' (traditional SSLRequest) or 'direct'
      sslnegotiation: void 0,
      application_name: void 0,
      fallback_application_name: void 0,
      options: void 0,
      parseInputDatesAsUTC: false,
      // max milliseconds any query using this connection will execute for before timing out in error.
      // false=unlimited
      statement_timeout: false,
      // Abort any statement that waits longer than the specified duration in milliseconds while attempting to acquire a lock.
      // false=unlimited
      lock_timeout: false,
      // Terminate any session with an open transaction that has been idle for longer than the specified duration in milliseconds
      // false=unlimited
      idle_in_transaction_session_timeout: false,
      // max milliseconds to wait for query to complete (client side)
      query_timeout: false,
      connect_timeout: 0,
      keepalives: 1,
      keepalives_idle: 0
    };
    var pgTypes = require_pg_types();
    var parseBigInteger = pgTypes.getTypeParser(20, "text");
    var parseBigIntegerArray = pgTypes.getTypeParser(1016, "text");
    module.exports.__defineSetter__("parseInt8", function(val) {
      pgTypes.setTypeParser(20, "text", val ? pgTypes.getTypeParser(23, "text") : parseBigInteger);
      pgTypes.setTypeParser(1016, "text", val ? pgTypes.getTypeParser(1007, "text") : parseBigIntegerArray);
    });
  }
});
var require_types = __commonJS({
  "node-built-in-modules:util/types"(exports, module) {
    module.exports = libDefault2;
  }
});
var require_utils = __commonJS({
  "node_modules/pg/lib/utils.js"(exports, module) {
    "use strict";
    var defaults2 = require_defaults();
    var { isDate } = require_types();
    function escapeElement(elementRepresentation) {
      const escaped = elementRepresentation.replace(/\\/g, "\\\\").replace(/"/g, '\\"');
      return '"' + escaped + '"';
    }
    __name(escapeElement, "escapeElement");
    __name2(escapeElement, "escapeElement");
    function arrayString(val) {
      let result = "{";
      for (let i = 0; i < val.length; i++) {
        if (i > 0) {
          result += ",";
        }
        let item = val[i];
        if (item == null) {
          result += "NULL";
        } else if (Array.isArray(item)) {
          result += arrayString(item);
        } else if (ArrayBuffer.isView(item)) {
          if (!(item instanceof Buffer)) {
            item = Buffer.from(item.buffer, item.byteOffset, item.byteLength);
          }
          result += "\\\\x" + item.toString("hex");
        } else {
          result += escapeElement(prepareValue(item));
        }
      }
      result += "}";
      return result;
    }
    __name(arrayString, "arrayString");
    __name2(arrayString, "arrayString");
    var prepareValue = /* @__PURE__ */ __name2(function(val, seen) {
      if (val == null) {
        return null;
      }
      if (typeof val === "object") {
        if (val instanceof Buffer) {
          return val;
        }
        if (ArrayBuffer.isView(val)) {
          return Buffer.from(val.buffer, val.byteOffset, val.byteLength);
        }
        if (isDate(val)) {
          if (defaults2.parseInputDatesAsUTC) {
            return dateToStringUTC(val);
          } else {
            return dateToString(val);
          }
        }
        if (Array.isArray(val)) {
          return arrayString(val);
        }
        return prepareObject(val, seen);
      }
      return val.toString();
    }, "prepareValue");
    function prepareObject(val, seen) {
      if (val && typeof val.toPostgres === "function") {
        seen = seen || [];
        if (seen.indexOf(val) !== -1) {
          throw new Error('circular reference detected while preparing "' + val + '" for query');
        }
        seen.push(val);
        return prepareValue(val.toPostgres(prepareValue), seen);
      }
      return JSON.stringify(val);
    }
    __name(prepareObject, "prepareObject");
    __name2(prepareObject, "prepareObject");
    function dateToString(date) {
      let offset = -date.getTimezoneOffset();
      let year = date.getFullYear();
      const isBCYear = year < 1;
      if (isBCYear) year = Math.abs(year) + 1;
      let ret = String(year).padStart(4, "0") + "-" + String(date.getMonth() + 1).padStart(2, "0") + "-" + String(date.getDate()).padStart(2, "0") + "T" + String(date.getHours()).padStart(2, "0") + ":" + String(date.getMinutes()).padStart(2, "0") + ":" + String(date.getSeconds()).padStart(2, "0") + "." + String(date.getMilliseconds()).padStart(3, "0");
      if (offset < 0) {
        ret += "-";
        offset *= -1;
      } else {
        ret += "+";
      }
      ret += String(Math.floor(offset / 60)).padStart(2, "0") + ":" + String(offset % 60).padStart(2, "0");
      if (isBCYear) ret += " BC";
      return ret;
    }
    __name(dateToString, "dateToString");
    __name2(dateToString, "dateToString");
    function dateToStringUTC(date) {
      let year = date.getUTCFullYear();
      const isBCYear = year < 1;
      if (isBCYear) year = Math.abs(year) + 1;
      let ret = String(year).padStart(4, "0") + "-" + String(date.getUTCMonth() + 1).padStart(2, "0") + "-" + String(date.getUTCDate()).padStart(2, "0") + "T" + String(date.getUTCHours()).padStart(2, "0") + ":" + String(date.getUTCMinutes()).padStart(2, "0") + ":" + String(date.getUTCSeconds()).padStart(2, "0") + "." + String(date.getUTCMilliseconds()).padStart(3, "0");
      ret += "+00:00";
      if (isBCYear) ret += " BC";
      return ret;
    }
    __name(dateToStringUTC, "dateToStringUTC");
    __name2(dateToStringUTC, "dateToStringUTC");
    function normalizeQueryConfig(config, values, callback) {
      config = typeof config === "string" ? { text: config } : config;
      if (values) {
        if (typeof values === "function") {
          config.callback = values;
        } else {
          config.values = values;
        }
      }
      if (callback) {
        config.callback = callback;
      }
      return config;
    }
    __name(normalizeQueryConfig, "normalizeQueryConfig");
    __name2(normalizeQueryConfig, "normalizeQueryConfig");
    var escapeIdentifier2 = /* @__PURE__ */ __name2(function(str) {
      return '"' + str.replace(/"/g, '""') + '"';
    }, "escapeIdentifier");
    var escapeLiteral2 = /* @__PURE__ */ __name2(function(str) {
      let hasBackslash = false;
      let escaped = "'";
      if (str == null) {
        return "''";
      }
      if (typeof str !== "string") {
        return "''";
      }
      for (let i = 0; i < str.length; i++) {
        const c = str[i];
        if (c === "'") {
          escaped += c + c;
        } else if (c === "\\") {
          escaped += c + c;
          hasBackslash = true;
        } else {
          escaped += c;
        }
      }
      escaped += "'";
      if (hasBackslash === true) {
        escaped = " E" + escaped;
      }
      return escaped;
    }, "escapeLiteral");
    module.exports = {
      prepareValue: /* @__PURE__ */ __name2(/* @__PURE__ */ __name(function prepareValueWrapper(value) {
        return prepareValue(value);
      }, "prepareValueWrapper"), "prepareValueWrapper"),
      normalizeQueryConfig,
      escapeIdentifier: escapeIdentifier2,
      escapeLiteral: escapeLiteral2
    };
  }
});
var require_util = __commonJS({
  "node-built-in-modules:util"(exports, module) {
    module.exports = libDefault3;
  }
});
var require_crypto = __commonJS({
  "node-built-in-modules:crypto"(exports, module) {
    module.exports = libDefault4;
  }
});
var require_utils2 = __commonJS({
  "node_modules/pg/lib/crypto/utils.js"(exports, module) {
    var nodeCrypto = require_crypto();
    module.exports = {
      postgresMd5PasswordHash,
      randomBytes,
      deriveKey,
      sha256,
      hashByName,
      hmacSha256,
      md5
    };
    var webCrypto = nodeCrypto.webcrypto || globalThis.crypto;
    var subtleCrypto = webCrypto.subtle;
    var textEncoder = new TextEncoder();
    function randomBytes(length) {
      return webCrypto.getRandomValues(Buffer.alloc(length));
    }
    __name(randomBytes, "randomBytes");
    __name2(randomBytes, "randomBytes");
    async function md5(string) {
      try {
        return nodeCrypto.createHash("md5").update(string, "utf-8").digest("hex");
      } catch (e) {
        const data = typeof string === "string" ? textEncoder.encode(string) : string;
        const hash = await subtleCrypto.digest("MD5", data);
        return Array.from(new Uint8Array(hash)).map((b) => b.toString(16).padStart(2, "0")).join("");
      }
    }
    __name(md5, "md5");
    __name2(md5, "md5");
    async function postgresMd5PasswordHash(user, password, salt) {
      const inner = await md5(password + user);
      const outer = await md5(Buffer.concat([Buffer.from(inner), salt]));
      return "md5" + outer;
    }
    __name(postgresMd5PasswordHash, "postgresMd5PasswordHash");
    __name2(postgresMd5PasswordHash, "postgresMd5PasswordHash");
    async function sha256(text) {
      return await subtleCrypto.digest("SHA-256", text);
    }
    __name(sha256, "sha256");
    __name2(sha256, "sha256");
    async function hashByName(hashName, text) {
      return await subtleCrypto.digest(hashName, text);
    }
    __name(hashByName, "hashByName");
    __name2(hashByName, "hashByName");
    async function hmacSha256(keyBuffer, msg) {
      const key = await subtleCrypto.importKey("raw", keyBuffer, { name: "HMAC", hash: "SHA-256" }, false, ["sign"]);
      return await subtleCrypto.sign("HMAC", key, textEncoder.encode(msg));
    }
    __name(hmacSha256, "hmacSha256");
    __name2(hmacSha256, "hmacSha256");
    async function deriveKey(password, salt, iterations) {
      const key = await subtleCrypto.importKey("raw", textEncoder.encode(password), "PBKDF2", false, ["deriveBits"]);
      const params = { name: "PBKDF2", hash: "SHA-256", salt, iterations };
      return await subtleCrypto.deriveBits(params, key, 32 * 8, ["deriveBits"]);
    }
    __name(deriveKey, "deriveKey");
    __name2(deriveKey, "deriveKey");
  }
});
var require_cert_signatures = __commonJS({
  "node_modules/pg/lib/crypto/cert-signatures.js"(exports, module) {
    function x509Error(msg, cert) {
      return new Error("SASL channel binding: " + msg + " when parsing public certificate " + cert.toString("base64"));
    }
    __name(x509Error, "x509Error");
    __name2(x509Error, "x509Error");
    function readASN1Length(data, index) {
      let length = data[index++];
      if (length < 128) return { length, index };
      const lengthBytes = length & 127;
      if (lengthBytes > 4) throw x509Error("bad length", data);
      length = 0;
      for (let i = 0; i < lengthBytes; i++) {
        length = length << 8 | data[index++];
      }
      return { length, index };
    }
    __name(readASN1Length, "readASN1Length");
    __name2(readASN1Length, "readASN1Length");
    function readASN1OID(data, index) {
      if (data[index++] !== 6) throw x509Error("non-OID data", data);
      const { length: OIDLength, index: indexAfterOIDLength } = readASN1Length(data, index);
      index = indexAfterOIDLength;
      const lastIndex = index + OIDLength;
      const byte1 = data[index++];
      let oid = (byte1 / 40 >> 0) + "." + byte1 % 40;
      while (index < lastIndex) {
        let value = 0;
        while (index < lastIndex) {
          const nextByte = data[index++];
          value = value << 7 | nextByte & 127;
          if (nextByte < 128) break;
        }
        oid += "." + value;
      }
      return { oid, index };
    }
    __name(readASN1OID, "readASN1OID");
    __name2(readASN1OID, "readASN1OID");
    function expectASN1Seq(data, index) {
      if (data[index++] !== 48) throw x509Error("non-sequence data", data);
      return readASN1Length(data, index);
    }
    __name(expectASN1Seq, "expectASN1Seq");
    __name2(expectASN1Seq, "expectASN1Seq");
    function signatureAlgorithmHashFromCertificate(data, index) {
      if (index === void 0) index = 0;
      index = expectASN1Seq(data, index).index;
      const { length: certInfoLength, index: indexAfterCertInfoLength } = expectASN1Seq(data, index);
      index = indexAfterCertInfoLength + certInfoLength;
      index = expectASN1Seq(data, index).index;
      const { oid, index: indexAfterOID } = readASN1OID(data, index);
      switch (oid) {
        // RSA
        case "1.2.840.113549.1.1.4":
          return "MD5";
        case "1.2.840.113549.1.1.5":
          return "SHA-1";
        case "1.2.840.113549.1.1.11":
          return "SHA-256";
        case "1.2.840.113549.1.1.12":
          return "SHA-384";
        case "1.2.840.113549.1.1.13":
          return "SHA-512";
        case "1.2.840.113549.1.1.14":
          return "SHA-224";
        case "1.2.840.113549.1.1.15":
          return "SHA512-224";
        case "1.2.840.113549.1.1.16":
          return "SHA512-256";
        // ECDSA
        case "1.2.840.10045.4.1":
          return "SHA-1";
        case "1.2.840.10045.4.3.1":
          return "SHA-224";
        case "1.2.840.10045.4.3.2":
          return "SHA-256";
        case "1.2.840.10045.4.3.3":
          return "SHA-384";
        case "1.2.840.10045.4.3.4":
          return "SHA-512";
        // RSASSA-PSS: hash is indicated separately
        case "1.2.840.113549.1.1.10": {
          index = indexAfterOID;
          index = expectASN1Seq(data, index).index;
          if (data[index++] !== 160) throw x509Error("non-tag data", data);
          index = readASN1Length(data, index).index;
          index = expectASN1Seq(data, index).index;
          const { oid: hashOID } = readASN1OID(data, index);
          switch (hashOID) {
            // standalone hash OIDs
            case "1.2.840.113549.2.5":
              return "MD5";
            case "1.3.14.3.2.26":
              return "SHA-1";
            case "2.16.840.1.101.3.4.2.1":
              return "SHA-256";
            case "2.16.840.1.101.3.4.2.2":
              return "SHA-384";
            case "2.16.840.1.101.3.4.2.3":
              return "SHA-512";
          }
          throw x509Error("unknown hash OID " + hashOID, data);
        }
        // Ed25519 -- see https: return//github.com/openssl/openssl/issues/15477
        case "1.3.101.110":
        case "1.3.101.112":
          return "SHA-512";
        // Ed448 -- still not in pg 17.2 (if supported, digest would be SHAKE256 x 64 bytes)
        case "1.3.101.111":
        case "1.3.101.113":
          throw x509Error("Ed448 certificate channel binding is not currently supported by Postgres");
      }
      throw x509Error("unknown OID " + oid, data);
    }
    __name(signatureAlgorithmHashFromCertificate, "signatureAlgorithmHashFromCertificate");
    __name2(signatureAlgorithmHashFromCertificate, "signatureAlgorithmHashFromCertificate");
    module.exports = { signatureAlgorithmHashFromCertificate };
  }
});
var require_sasl = __commonJS({
  "node_modules/pg/lib/crypto/sasl.js"(exports, module) {
    "use strict";
    var crypto2 = require_utils2();
    var { signatureAlgorithmHashFromCertificate } = require_cert_signatures();
    function saslprep(password) {
      const nonAsciiSpace = /[\u00A0\u1680\u2000-\u200B\u202F\u205F\u3000]/g;
      const mappedToNothing = /[\u00AD\u034F\u1806\u180B\u180C\u180D\u200C\u200D\u2060\uFE00-\uFE0F\uFEFF]/g;
      return password.replace(nonAsciiSpace, " ").replace(mappedToNothing, "").normalize("NFKC");
    }
    __name(saslprep, "saslprep");
    __name2(saslprep, "saslprep");
    var DEFAULT_MAX_SCRAM_ITERATIONS = 1e5;
    function startSession(mechanisms, stream, scramMaxIterations = DEFAULT_MAX_SCRAM_ITERATIONS) {
      const candidates = ["SCRAM-SHA-256"];
      if (stream) candidates.unshift("SCRAM-SHA-256-PLUS");
      const mechanism = candidates.find((candidate) => mechanisms.includes(candidate));
      if (!mechanism) {
        throw new Error("SASL: Only mechanism(s) " + candidates.join(" and ") + " are supported");
      }
      if (mechanism === "SCRAM-SHA-256-PLUS" && typeof stream.getPeerCertificate !== "function") {
        throw new Error("SASL: Mechanism SCRAM-SHA-256-PLUS requires a certificate");
      }
      const clientNonce = crypto2.randomBytes(18).toString("base64");
      const gs2Header = mechanism === "SCRAM-SHA-256-PLUS" ? "p=tls-server-end-point" : stream ? "y" : "n";
      return {
        mechanism,
        clientNonce,
        response: gs2Header + ",,n=*,r=" + clientNonce,
        message: "SASLInitialResponse",
        scramMaxIterations
      };
    }
    __name(startSession, "startSession");
    __name2(startSession, "startSession");
    async function continueSession(session, password, serverData, stream) {
      if (session.message !== "SASLInitialResponse") {
        throw new Error("SASL: Last message was not SASLInitialResponse");
      }
      if (typeof password !== "string") {
        throw new Error("SASL: SCRAM-SERVER-FIRST-MESSAGE: client password must be a string");
      }
      if (password === "") {
        throw new Error("SASL: SCRAM-SERVER-FIRST-MESSAGE: client password must be a non-empty string");
      }
      if (typeof serverData !== "string") {
        throw new Error("SASL: SCRAM-SERVER-FIRST-MESSAGE: serverData must be a string");
      }
      const sv = parseServerFirstMessage(serverData);
      if (!sv.nonce.startsWith(session.clientNonce)) {
        throw new Error("SASL: SCRAM-SERVER-FIRST-MESSAGE: server nonce does not start with client nonce");
      } else if (sv.nonce.length === session.clientNonce.length) {
        throw new Error("SASL: SCRAM-SERVER-FIRST-MESSAGE: server nonce is too short");
      }
      const scramMaxIterations = typeof session.scramMaxIterations === "number" ? session.scramMaxIterations : DEFAULT_MAX_SCRAM_ITERATIONS;
      if (scramMaxIterations !== 0 && sv.iteration > scramMaxIterations) {
        throw new Error(
          "SASL: SCRAM-SERVER-FIRST-MESSAGE: iteration count " + sv.iteration + " exceeds scramMaxIterations of " + scramMaxIterations
        );
      }
      const clientFirstMessageBare = "n=*,r=" + session.clientNonce;
      const serverFirstMessage = "r=" + sv.nonce + ",s=" + sv.salt + ",i=" + sv.iteration;
      let channelBinding = stream ? "eSws" : "biws";
      if (session.mechanism === "SCRAM-SHA-256-PLUS") {
        const peerCert = stream.getPeerCertificate().raw;
        let hashName = signatureAlgorithmHashFromCertificate(peerCert);
        if (hashName === "MD5" || hashName === "SHA-1") hashName = "SHA-256";
        const certHash = await crypto2.hashByName(hashName, peerCert);
        const bindingData = Buffer.concat([Buffer.from("p=tls-server-end-point,,"), Buffer.from(certHash)]);
        channelBinding = bindingData.toString("base64");
      }
      const clientFinalMessageWithoutProof = "c=" + channelBinding + ",r=" + sv.nonce;
      const authMessage = clientFirstMessageBare + "," + serverFirstMessage + "," + clientFinalMessageWithoutProof;
      const saltBytes = Buffer.from(sv.salt, "base64");
      const saltedPassword = await crypto2.deriveKey(saslprep(password), saltBytes, sv.iteration);
      const clientKey = await crypto2.hmacSha256(saltedPassword, "Client Key");
      const storedKey = await crypto2.sha256(clientKey);
      const clientSignature = await crypto2.hmacSha256(storedKey, authMessage);
      const clientProof = xorBuffers(Buffer.from(clientKey), Buffer.from(clientSignature)).toString("base64");
      const serverKey = await crypto2.hmacSha256(saltedPassword, "Server Key");
      const serverSignatureBytes = await crypto2.hmacSha256(serverKey, authMessage);
      session.message = "SASLResponse";
      session.serverSignature = Buffer.from(serverSignatureBytes).toString("base64");
      session.response = clientFinalMessageWithoutProof + ",p=" + clientProof;
    }
    __name(continueSession, "continueSession");
    __name2(continueSession, "continueSession");
    function finalizeSession(session, serverData) {
      if (session.message !== "SASLResponse") {
        throw new Error("SASL: Last message was not SASLResponse");
      }
      if (typeof serverData !== "string") {
        throw new Error("SASL: SCRAM-SERVER-FINAL-MESSAGE: serverData must be a string");
      }
      const { serverSignature } = parseServerFinalMessage(serverData);
      if (serverSignature !== session.serverSignature) {
        throw new Error("SASL: SCRAM-SERVER-FINAL-MESSAGE: server signature does not match");
      }
    }
    __name(finalizeSession, "finalizeSession");
    __name2(finalizeSession, "finalizeSession");
    function isPrintableChars(text) {
      if (typeof text !== "string") {
        throw new TypeError("SASL: text must be a string");
      }
      return text.split("").map((_, i) => text.charCodeAt(i)).every((c) => c >= 33 && c <= 43 || c >= 45 && c <= 126);
    }
    __name(isPrintableChars, "isPrintableChars");
    __name2(isPrintableChars, "isPrintableChars");
    function isBase64(text) {
      return /^(?:[a-zA-Z0-9+/]{4})*(?:[a-zA-Z0-9+/]{2}==|[a-zA-Z0-9+/]{3}=)?$/.test(text);
    }
    __name(isBase64, "isBase64");
    __name2(isBase64, "isBase64");
    function parseAttributePairs(text) {
      if (typeof text !== "string") {
        throw new TypeError("SASL: attribute pairs text must be a string");
      }
      return new Map(
        text.split(",").map((attrValue) => {
          if (!/^.=/.test(attrValue)) {
            throw new Error("SASL: Invalid attribute pair entry");
          }
          const name = attrValue[0];
          const value = attrValue.substring(2);
          return [name, value];
        })
      );
    }
    __name(parseAttributePairs, "parseAttributePairs");
    __name2(parseAttributePairs, "parseAttributePairs");
    function parseServerFirstMessage(data) {
      const attrPairs = parseAttributePairs(data);
      const nonce = attrPairs.get("r");
      if (!nonce) {
        throw new Error("SASL: SCRAM-SERVER-FIRST-MESSAGE: nonce missing");
      } else if (!isPrintableChars(nonce)) {
        throw new Error("SASL: SCRAM-SERVER-FIRST-MESSAGE: nonce must only contain printable characters");
      }
      const salt = attrPairs.get("s");
      if (!salt) {
        throw new Error("SASL: SCRAM-SERVER-FIRST-MESSAGE: salt missing");
      } else if (!isBase64(salt)) {
        throw new Error("SASL: SCRAM-SERVER-FIRST-MESSAGE: salt must be base64");
      }
      const iterationText = attrPairs.get("i");
      if (!iterationText) {
        throw new Error("SASL: SCRAM-SERVER-FIRST-MESSAGE: iteration missing");
      } else if (!/^[1-9][0-9]*$/.test(iterationText)) {
        throw new Error("SASL: SCRAM-SERVER-FIRST-MESSAGE: invalid iteration count");
      }
      const iteration = parseInt(iterationText, 10);
      return {
        nonce,
        salt,
        iteration
      };
    }
    __name(parseServerFirstMessage, "parseServerFirstMessage");
    __name2(parseServerFirstMessage, "parseServerFirstMessage");
    function parseServerFinalMessage(serverData) {
      const attrPairs = parseAttributePairs(serverData);
      const error = attrPairs.get("e");
      const serverSignature = attrPairs.get("v");
      if (error) {
        throw new Error(`SASL: SCRAM-SERVER-FINAL-MESSAGE: server returned error: "${error}"`);
      }
      if (!serverSignature) {
        throw new Error("SASL: SCRAM-SERVER-FINAL-MESSAGE: server signature is missing");
      } else if (!isBase64(serverSignature)) {
        throw new Error("SASL: SCRAM-SERVER-FINAL-MESSAGE: server signature must be base64");
      }
      return {
        serverSignature
      };
    }
    __name(parseServerFinalMessage, "parseServerFinalMessage");
    __name2(parseServerFinalMessage, "parseServerFinalMessage");
    function xorBuffers(a, b) {
      if (!Buffer.isBuffer(a)) {
        throw new TypeError("first argument must be a Buffer");
      }
      if (!Buffer.isBuffer(b)) {
        throw new TypeError("second argument must be a Buffer");
      }
      if (a.length !== b.length) {
        throw new Error("Buffer lengths must match");
      }
      if (a.length === 0) {
        throw new Error("Buffers cannot be empty");
      }
      return Buffer.from(a.map((_, i) => a[i] ^ b[i]));
    }
    __name(xorBuffers, "xorBuffers");
    __name2(xorBuffers, "xorBuffers");
    module.exports = {
      startSession,
      continueSession,
      finalizeSession,
      DEFAULT_MAX_SCRAM_ITERATIONS
    };
  }
});
var require_type_overrides = __commonJS({
  "node_modules/pg/lib/type-overrides.js"(exports, module) {
    "use strict";
    var types2 = require_pg_types();
    function TypeOverrides2(userTypes) {
      this._types = userTypes || types2;
      this.text = {};
      this.binary = {};
    }
    __name(TypeOverrides2, "TypeOverrides2");
    __name2(TypeOverrides2, "TypeOverrides");
    TypeOverrides2.prototype.getOverrides = function(format) {
      switch (format) {
        case "text":
          return this.text;
        case "binary":
          return this.binary;
        default:
          return {};
      }
    };
    TypeOverrides2.prototype.setTypeParser = function(oid, format, parseFn) {
      if (typeof format === "function") {
        parseFn = format;
        format = "text";
      }
      this.getOverrides(format)[oid] = parseFn;
    };
    TypeOverrides2.prototype.getTypeParser = function(oid, format) {
      format = format || "text";
      return this.getOverrides(format)[oid] || this._types.getTypeParser(oid, format);
    };
    module.exports = TypeOverrides2;
  }
});
var require_dns = __commonJS({
  "node-built-in-modules:dns"(exports, module) {
    module.exports = libDefault5;
  }
});
var require_fs = __commonJS({
  "node-built-in-modules:fs"(exports, module) {
    module.exports = libDefault6;
  }
});
var require_pg_connection_string = __commonJS({
  "node_modules/pg-connection-string/index.js"(exports, module) {
    "use strict";
    function parse(str, options = {}) {
      if (str.charAt(0) === "/") {
        const config2 = str.split(" ");
        return { host: config2[0], database: config2[1] };
      }
      const config = /* @__PURE__ */ Object.create(null);
      let result;
      let dummyHost = false;
      if (/ |%[^a-f0-9]|%[a-f0-9][^a-f0-9]/i.test(str)) {
        str = encodeURI(str).replace(/%25(\d\d)/g, "%$1");
      }
      try {
        try {
          result = new URL(str, "postgres://base");
        } catch (e) {
          result = new URL(str.replace("@/", "@___DUMMY___/"), "postgres://base");
          dummyHost = true;
        }
      } catch (err) {
        err.input && (err.input = "*****REDACTED*****");
        throw err;
      }
      for (const entry of result.searchParams.entries()) {
        config[entry[0]] = entry[1];
      }
      config.user = config.user || decodeURIComponent(result.username);
      config.password = config.password || decodeURIComponent(result.password);
      if (result.protocol == "socket:") {
        config.host = decodeURI(result.pathname);
        config.database = result.searchParams.get("db");
        config.client_encoding = result.searchParams.get("encoding");
        return config;
      }
      const hostname = dummyHost ? "" : result.hostname;
      if (!config.host) {
        config.host = decodeURIComponent(hostname);
      } else if (hostname && /^%2f/i.test(hostname)) {
        result.pathname = hostname + result.pathname;
      }
      if (!config.port) {
        config.port = result.port;
      }
      const pathname = result.pathname.slice(1) || null;
      config.database = pathname ? decodeURI(pathname) : null;
      if (config.ssl === "true" || config.ssl === "1") {
        config.ssl = true;
      }
      if (config.ssl === "0") {
        config.ssl = false;
      }
      if (config.sslcert || config.sslkey || config.sslrootcert || config.sslmode) {
        config.ssl = {};
      }
      if (config.sslnegotiation === "direct" && config.ssl === void 0) {
        config.ssl = true;
      }
      const fs = config.sslcert || config.sslkey || config.sslrootcert ? require_fs() : null;
      if (config.sslcert) {
        config.ssl.cert = fs.readFileSync(config.sslcert).toString();
      }
      if (config.sslkey) {
        config.ssl.key = fs.readFileSync(config.sslkey).toString();
      }
      if (config.sslrootcert) {
        config.ssl.ca = fs.readFileSync(config.sslrootcert).toString();
      }
      if (options.useLibpqCompat && config.uselibpqcompat) {
        throw new Error("Both useLibpqCompat and uselibpqcompat are set. Please use only one of them.");
      }
      if (config.uselibpqcompat === "true" || options.useLibpqCompat) {
        switch (config.sslmode) {
          case "disable": {
            config.ssl = false;
            break;
          }
          case "prefer": {
            config.ssl.rejectUnauthorized = false;
            break;
          }
          case "require": {
            if (config.sslrootcert) {
              config.ssl.checkServerIdentity = function() {
              };
            } else {
              config.ssl.rejectUnauthorized = false;
            }
            break;
          }
          case "verify-ca": {
            if (!config.ssl.ca) {
              throw new Error(
                "SECURITY WARNING: Using sslmode=verify-ca requires specifying a CA with sslrootcert. If a public CA is used, verify-ca allows connections to a server that somebody else may have registered with the CA, making you vulnerable to Man-in-the-Middle attacks. Either specify a custom CA certificate with sslrootcert parameter or use sslmode=verify-full for proper security."
              );
            }
            config.ssl.checkServerIdentity = function() {
            };
            break;
          }
          case "verify-full": {
            break;
          }
        }
      } else {
        switch (config.sslmode) {
          case "disable": {
            config.ssl = false;
            break;
          }
          case "prefer":
          case "require":
          case "verify-ca":
          case "verify-full": {
            if (config.sslmode !== "verify-full") {
              deprecatedSslModeWarning(config.sslmode);
            }
            break;
          }
          case "no-verify": {
            config.ssl.rejectUnauthorized = false;
            break;
          }
        }
      }
      return config;
    }
    __name(parse, "parse");
    __name2(parse, "parse");
    function toConnectionOptions(sslConfig) {
      const connectionOptions = Object.entries(sslConfig).reduce((c, [key, value]) => {
        if (value !== void 0 && value !== null) {
          c[key] = value;
        }
        return c;
      }, /* @__PURE__ */ Object.create(null));
      return connectionOptions;
    }
    __name(toConnectionOptions, "toConnectionOptions");
    __name2(toConnectionOptions, "toConnectionOptions");
    function toClientConfig(config) {
      const poolConfig = Object.entries(config).reduce((c, [key, value]) => {
        if (key === "ssl") {
          const sslConfig = value;
          if (typeof sslConfig === "boolean") {
            c[key] = sslConfig;
          }
          if (typeof sslConfig === "object") {
            c[key] = toConnectionOptions(sslConfig);
          }
        } else if (value !== void 0 && value !== null) {
          if (key === "port") {
            if (value !== "") {
              const v = parseInt(value, 10);
              if (isNaN(v)) {
                throw new Error(`Invalid ${key}: ${value}`);
              }
              c[key] = v;
            }
          } else {
            c[key] = value;
          }
        }
        return c;
      }, /* @__PURE__ */ Object.create(null));
      return poolConfig;
    }
    __name(toClientConfig, "toClientConfig");
    __name2(toClientConfig, "toClientConfig");
    function parseIntoClientConfig(str) {
      return toClientConfig(parse(str));
    }
    __name(parseIntoClientConfig, "parseIntoClientConfig");
    __name2(parseIntoClientConfig, "parseIntoClientConfig");
    function deprecatedSslModeWarning(sslmode) {
      if (!deprecatedSslModeWarning.warned && typeof process !== "undefined" && process.emitWarning) {
        deprecatedSslModeWarning.warned = true;
        process.emitWarning(`SECURITY WARNING: The SSL modes 'prefer', 'require', and 'verify-ca' are treated as aliases for 'verify-full'.
In the next major version (pg-connection-string v3.0.0 and pg v9.0.0), these modes will adopt standard libpq semantics, which have weaker security guarantees.

To prepare for this change:
- If you want the current behavior, explicitly use 'sslmode=verify-full'
- If you want libpq compatibility now, use 'uselibpqcompat=true&sslmode=${sslmode}'

See https://www.postgresql.org/docs/current/libpq-ssl.html for libpq SSL mode definitions.`);
      }
    }
    __name(deprecatedSslModeWarning, "deprecatedSslModeWarning");
    __name2(deprecatedSslModeWarning, "deprecatedSslModeWarning");
    module.exports = parse;
    parse.parse = parse;
    parse.toClientConfig = toClientConfig;
    parse.parseIntoClientConfig = parseIntoClientConfig;
  }
});
var require_connection_parameters = __commonJS({
  "node_modules/pg/lib/connection-parameters.js"(exports, module) {
    "use strict";
    var dns = require_dns();
    var defaults2 = require_defaults();
    var parse = require_pg_connection_string().parse;
    var val = /* @__PURE__ */ __name2(function(key, config, envVar) {
      if (config[key]) {
        return config[key];
      }
      if (envVar === void 0) {
        envVar = process.env["PG" + key.toUpperCase()];
      } else if (envVar === false) {
      } else {
        envVar = process.env[envVar];
      }
      return envVar || defaults2[key];
    }, "val");
    var readSSLConfigFromEnvironment = /* @__PURE__ */ __name2(function() {
      switch (process.env.PGSSLMODE) {
        case "disable":
          return false;
        case "prefer":
        case "require":
        case "verify-ca":
        case "verify-full":
          return true;
        case "no-verify":
          return { rejectUnauthorized: false };
      }
      return defaults2.ssl;
    }, "readSSLConfigFromEnvironment");
    var quoteParamValue = /* @__PURE__ */ __name2(function(value) {
      return "'" + ("" + value).replace(/\\/g, "\\\\").replace(/'/g, "\\'") + "'";
    }, "quoteParamValue");
    var add = /* @__PURE__ */ __name2(function(params, config, paramName) {
      const value = config[paramName];
      if (value !== void 0 && value !== null) {
        params.push(paramName + "=" + quoteParamValue(value));
      }
    }, "add");
    var ConnectionParameters = class {
      static {
        __name(this, "ConnectionParameters");
      }
      static {
        __name2(this, "ConnectionParameters");
      }
      constructor(config) {
        config = typeof config === "string" ? parse(config) : config || {};
        if (config.connectionString) {
          config = Object.assign({}, config, parse(config.connectionString));
        }
        this.user = val("user", config);
        this.database = val("database", config);
        if (this.database === void 0) {
          this.database = this.user;
        }
        this.port = parseInt(val("port", config), 10);
        this.host = val("host", config);
        Object.defineProperty(this, "password", {
          configurable: true,
          enumerable: false,
          writable: true,
          value: val("password", config)
        });
        this.binary = val("binary", config);
        this.options = val("options", config);
        this.ssl = typeof config.ssl === "undefined" ? readSSLConfigFromEnvironment() : config.ssl;
        if (typeof this.ssl === "string") {
          if (this.ssl === "true") {
            this.ssl = true;
          }
        }
        if (this.ssl === "no-verify") {
          this.ssl = { rejectUnauthorized: false };
        }
        if (this.ssl && this.ssl.key) {
          Object.defineProperty(this.ssl, "key", {
            enumerable: false
          });
        }
        this.sslnegotiation = val("sslnegotiation", config, "PGSSLNEGOTIATION");
        if (this.sslnegotiation !== void 0 && this.sslnegotiation !== "postgres" && this.sslnegotiation !== "direct") {
          throw new Error(
            `Invalid sslnegotiation value: "${this.sslnegotiation}". Valid values are "postgres" and "direct".`
          );
        }
        if (this.sslnegotiation === "direct" && !this.ssl) {
          throw new Error("sslnegotiation=direct requires SSL to be enabled");
        }
        this.client_encoding = val("client_encoding", config);
        this.replication = val("replication", config);
        this.isDomainSocket = !(this.host || "").indexOf("/");
        this.application_name = val("application_name", config, "PGAPPNAME");
        this.fallback_application_name = val("fallback_application_name", config, false);
        this.statement_timeout = val("statement_timeout", config, false);
        this.lock_timeout = val("lock_timeout", config, false);
        this.idle_in_transaction_session_timeout = val("idle_in_transaction_session_timeout", config, false);
        this.query_timeout = val("query_timeout", config, false);
        if (config.connectionTimeoutMillis === void 0) {
          this.connect_timeout = process.env.PGCONNECT_TIMEOUT || 0;
        } else {
          this.connect_timeout = Math.floor(config.connectionTimeoutMillis / 1e3);
        }
        if (config.keepAlive === false) {
          this.keepalives = 0;
        } else if (config.keepAlive === true) {
          this.keepalives = 1;
        }
        if (typeof config.keepAliveInitialDelayMillis === "number") {
          this.keepalives_idle = Math.floor(config.keepAliveInitialDelayMillis / 1e3);
        }
      }
      getLibpqConnectionString(cb) {
        const params = [];
        add(params, this, "user");
        add(params, this, "password");
        add(params, this, "port");
        add(params, this, "application_name");
        add(params, this, "fallback_application_name");
        add(params, this, "connect_timeout");
        add(params, this, "options");
        const ssl = typeof this.ssl === "object" ? this.ssl : this.ssl ? { sslmode: this.ssl } : {};
        add(params, ssl, "sslmode");
        add(params, ssl, "sslca");
        add(params, ssl, "sslkey");
        add(params, ssl, "sslcert");
        add(params, ssl, "sslrootcert");
        add(params, this, "sslnegotiation");
        if (this.database) {
          params.push("dbname=" + quoteParamValue(this.database));
        }
        if (this.replication) {
          params.push("replication=" + quoteParamValue(this.replication));
        }
        if (this.host) {
          params.push("host=" + quoteParamValue(this.host));
        }
        if (this.isDomainSocket) {
          return cb(null, params.join(" "));
        }
        if (this.client_encoding) {
          params.push("client_encoding=" + quoteParamValue(this.client_encoding));
        }
        dns.lookup(this.host, function(err, address) {
          if (err) return cb(err, null);
          params.push("hostaddr=" + quoteParamValue(address));
          return cb(null, params.join(" "));
        });
      }
    };
    module.exports = ConnectionParameters;
  }
});
var require_result = __commonJS({
  "node_modules/pg/lib/result.js"(exports, module) {
    "use strict";
    var types2 = require_pg_types();
    var matchRegexp = /^([A-Za-z]+)(?: (\d+))?(?: (\d+))?/;
    var Result2 = class {
      static {
        __name(this, "Result2");
      }
      static {
        __name2(this, "Result");
      }
      constructor(rowMode, types3) {
        this.command = null;
        this.rowCount = null;
        this.oid = null;
        this.rows = [];
        this.fields = [];
        this._parsers = void 0;
        this._types = types3;
        this.RowCtor = null;
        this.rowAsArray = rowMode === "array";
        if (this.rowAsArray) {
          this.parseRow = this._parseRowAsArray;
        }
        this._prebuiltEmptyResultObject = null;
      }
      // adds a command complete message
      addCommandComplete(msg) {
        let match;
        if (msg.text) {
          match = matchRegexp.exec(msg.text);
        } else {
          match = matchRegexp.exec(msg.command);
        }
        if (match) {
          this.command = match[1];
          if (match[3]) {
            this.oid = parseInt(match[2], 10);
            this.rowCount = parseInt(match[3], 10);
          } else if (match[2]) {
            this.rowCount = parseInt(match[2], 10);
          }
        }
      }
      _parseRowAsArray(rowData) {
        const row = new Array(rowData.length);
        for (let i = 0, len = rowData.length; i < len; i++) {
          const rawValue = rowData[i];
          if (rawValue !== null) {
            row[i] = this._parsers[i](rawValue);
          } else {
            row[i] = null;
          }
        }
        return row;
      }
      parseRow(rowData) {
        const row = { ...this._prebuiltEmptyResultObject };
        for (let i = 0, len = rowData.length; i < len; i++) {
          const rawValue = rowData[i];
          const field = this.fields[i].name;
          if (rawValue !== null) {
            const v = this.fields[i].format === "binary" ? Buffer.from(rawValue) : rawValue;
            row[field] = this._parsers[i](v);
          } else {
            row[field] = null;
          }
        }
        return row;
      }
      addRow(row) {
        this.rows.push(row);
      }
      addFields(fieldDescriptions) {
        this.fields = fieldDescriptions;
        if (this.fields.length) {
          this._parsers = new Array(fieldDescriptions.length);
        }
        const row = /* @__PURE__ */ Object.create(null);
        for (let i = 0; i < fieldDescriptions.length; i++) {
          const desc = fieldDescriptions[i];
          row[desc.name] = null;
          if (this._types) {
            this._parsers[i] = this._types.getTypeParser(desc.dataTypeID, desc.format || "text");
          } else {
            this._parsers[i] = types2.getTypeParser(desc.dataTypeID, desc.format || "text");
          }
        }
        this._prebuiltEmptyResultObject = { ...row };
      }
    };
    module.exports = Result2;
  }
});
var require_query = __commonJS({
  "node_modules/pg/lib/query.js"(exports, module) {
    "use strict";
    var { EventEmitter } = require_events();
    var Result2 = require_result();
    var utils = require_utils();
    var Query2 = class extends EventEmitter {
      static {
        __name(this, "Query2");
      }
      static {
        __name2(this, "Query");
      }
      constructor(config, values, callback) {
        super();
        config = utils.normalizeQueryConfig(config, values, callback);
        this.text = config.text;
        this.values = config.values;
        this.rows = config.rows;
        this.types = config.types;
        this.name = config.name;
        this.queryMode = config.queryMode;
        this.binary = config.binary;
        this.portal = config.portal || "";
        this.callback = config.callback;
        this._rowMode = config.rowMode;
        if (process.domain && config.callback) {
          this.callback = process.domain.bind(config.callback);
        }
        this._result = new Result2(this._rowMode, this.types);
        this._results = this._result;
        this._canceledDueToError = false;
      }
      requiresPreparation() {
        if (this.queryMode === "extended") {
          return true;
        }
        if (this.name) {
          return true;
        }
        if (this.rows) {
          return true;
        }
        if (!this.text) {
          return false;
        }
        if (!this.values) {
          return false;
        }
        return this.values.length > 0;
      }
      _checkForMultirow() {
        if (this._result.command) {
          if (!Array.isArray(this._results)) {
            this._results = [this._result];
          }
          this._result = new Result2(this._rowMode, this._result._types);
          this._results.push(this._result);
        }
      }
      // associates row metadata from the supplied
      // message with this query object
      // metadata used when parsing row results
      handleRowDescription(msg) {
        this._checkForMultirow();
        this._result.addFields(msg.fields);
        this._accumulateRows = this.callback || !this.listeners("row").length;
      }
      handleDataRow(msg) {
        let row;
        if (this._canceledDueToError) {
          return;
        }
        try {
          row = this._result.parseRow(msg.fields);
        } catch (err) {
          this._canceledDueToError = err;
          return;
        }
        this.emit("row", row, this._result);
        if (this._accumulateRows) {
          this._result.addRow(row);
        }
      }
      handleCommandComplete(msg, connection) {
        this._checkForMultirow();
        this._result.addCommandComplete(msg);
        if (this.rows) {
          connection.sync();
        }
      }
      // if a named prepared statement is created with empty query text
      // the backend will send an emptyQuery message but *not* a command complete message
      // since we pipeline sync immediately after execute we don't need to do anything here
      // unless we have rows specified, in which case we did not pipeline the initial sync call
      handleEmptyQuery(connection) {
        if (this.rows) {
          connection.sync();
        }
      }
      handleError(err, connection) {
        if (this._canceledDueToError) {
          err = this._canceledDueToError;
          this._canceledDueToError = false;
        }
        if (this.callback) {
          return this.callback(err);
        }
        this.emit("error", err);
      }
      handleReadyForQuery(con) {
        if (this._canceledDueToError) {
          return this.handleError(this._canceledDueToError, con);
        }
        if (this.callback) {
          try {
            this.callback(null, this._results);
          } catch (err) {
            process.nextTick(() => {
              throw err;
            });
          }
        }
        this.emit("end", this._results);
      }
      submit(connection) {
        if (typeof this.text !== "string" && typeof this.name !== "string") {
          return new Error("A query must have either text or a name. Supplying neither is unsupported.");
        }
        const previous = connection.parsedStatements[this.name];
        if (this.text && previous && this.text !== previous) {
          return new Error(`Prepared statements must be unique - '${this.name}' was used for a different statement`);
        }
        if (this.values && !Array.isArray(this.values)) {
          return new Error("Query values must be an array");
        }
        if (this.requiresPreparation()) {
          connection.stream.cork && connection.stream.cork();
          try {
            this.prepare(connection);
          } finally {
            connection.stream.uncork && connection.stream.uncork();
          }
        } else {
          connection.query(this.text);
        }
        return null;
      }
      hasBeenParsed(connection) {
        return this.name && connection.parsedStatements[this.name];
      }
      handlePortalSuspended(connection) {
        this._getRows(connection, this.rows);
      }
      _getRows(connection, rows) {
        connection.execute({
          portal: this.portal,
          rows
        });
        if (!rows) {
          connection.sync();
        } else {
          connection.flush();
        }
      }
      // http://developer.postgresql.org/pgdocs/postgres/protocol-flow.html#PROTOCOL-FLOW-EXT-QUERY
      prepare(connection) {
        if (!this.hasBeenParsed(connection)) {
          connection.parse({
            text: this.text,
            name: this.name,
            types: this.types
          });
        }
        try {
          connection.bind({
            portal: this.portal,
            statement: this.name,
            values: this.values,
            binary: this.binary,
            valueMapper: utils.prepareValue
          });
        } catch (err) {
          connection.close({ type: "S", name: this.name });
          connection.sync();
          this.handleError(err, connection);
          return;
        }
        connection.describe({
          type: "P",
          name: this.portal || ""
        });
        this._getRows(connection, this.rows);
      }
      handleCopyInResponse(connection) {
        connection.sendCopyFail("No source stream defined");
      }
      handleCopyData(msg, connection) {
      }
    };
    module.exports = Query2;
  }
});
var require_messages = __commonJS({
  "node_modules/pg-protocol/dist/messages.js"(exports) {
    "use strict";
    Object.defineProperty(exports, "__esModule", { value: true });
    exports.NoticeMessage = exports.DataRowMessage = exports.CommandCompleteMessage = exports.ReadyForQueryMessage = exports.NotificationResponseMessage = exports.BackendKeyDataMessage = exports.AuthenticationMD5Password = exports.ParameterStatusMessage = exports.ParameterDescriptionMessage = exports.RowDescriptionMessage = exports.Field = exports.CopyResponse = exports.CopyDataMessage = exports.DatabaseError = exports.copyDone = exports.emptyQuery = exports.replicationStart = exports.portalSuspended = exports.noData = exports.closeComplete = exports.bindComplete = exports.parseComplete = void 0;
    exports.parseComplete = {
      name: "parseComplete",
      length: 5
    };
    exports.bindComplete = {
      name: "bindComplete",
      length: 5
    };
    exports.closeComplete = {
      name: "closeComplete",
      length: 5
    };
    exports.noData = {
      name: "noData",
      length: 5
    };
    exports.portalSuspended = {
      name: "portalSuspended",
      length: 5
    };
    exports.replicationStart = {
      name: "replicationStart",
      length: 4
    };
    exports.emptyQuery = {
      name: "emptyQuery",
      length: 4
    };
    exports.copyDone = {
      name: "copyDone",
      length: 4
    };
    var DatabaseError2 = class extends Error {
      static {
        __name(this, "DatabaseError2");
      }
      static {
        __name2(this, "DatabaseError");
      }
      constructor(message, length, name) {
        super(message);
        this.length = length;
        this.name = name;
      }
    };
    exports.DatabaseError = DatabaseError2;
    var CopyDataMessage = class {
      static {
        __name(this, "CopyDataMessage");
      }
      static {
        __name2(this, "CopyDataMessage");
      }
      constructor(length, chunk) {
        this.length = length;
        this.chunk = chunk;
        this.name = "copyData";
      }
    };
    exports.CopyDataMessage = CopyDataMessage;
    var CopyResponse = class {
      static {
        __name(this, "CopyResponse");
      }
      static {
        __name2(this, "CopyResponse");
      }
      constructor(length, name, binary, columnCount) {
        this.length = length;
        this.name = name;
        this.binary = binary;
        this.columnTypes = new Array(columnCount);
      }
    };
    exports.CopyResponse = CopyResponse;
    var Field = class {
      static {
        __name(this, "Field");
      }
      static {
        __name2(this, "Field");
      }
      constructor(name, tableID, columnID, dataTypeID, dataTypeSize, dataTypeModifier, format) {
        this.name = name;
        this.tableID = tableID;
        this.columnID = columnID;
        this.dataTypeID = dataTypeID;
        this.dataTypeSize = dataTypeSize;
        this.dataTypeModifier = dataTypeModifier;
        this.format = format;
      }
    };
    exports.Field = Field;
    var RowDescriptionMessage = class {
      static {
        __name(this, "RowDescriptionMessage");
      }
      static {
        __name2(this, "RowDescriptionMessage");
      }
      constructor(length, fieldCount) {
        this.length = length;
        this.fieldCount = fieldCount;
        this.name = "rowDescription";
        this.fields = new Array(this.fieldCount);
      }
    };
    exports.RowDescriptionMessage = RowDescriptionMessage;
    var ParameterDescriptionMessage = class {
      static {
        __name(this, "ParameterDescriptionMessage");
      }
      static {
        __name2(this, "ParameterDescriptionMessage");
      }
      constructor(length, parameterCount) {
        this.length = length;
        this.parameterCount = parameterCount;
        this.name = "parameterDescription";
        this.dataTypeIDs = new Array(this.parameterCount);
      }
    };
    exports.ParameterDescriptionMessage = ParameterDescriptionMessage;
    var ParameterStatusMessage = class {
      static {
        __name(this, "ParameterStatusMessage");
      }
      static {
        __name2(this, "ParameterStatusMessage");
      }
      constructor(length, parameterName, parameterValue) {
        this.length = length;
        this.parameterName = parameterName;
        this.parameterValue = parameterValue;
        this.name = "parameterStatus";
      }
    };
    exports.ParameterStatusMessage = ParameterStatusMessage;
    var AuthenticationMD5Password = class {
      static {
        __name(this, "AuthenticationMD5Password");
      }
      static {
        __name2(this, "AuthenticationMD5Password");
      }
      constructor(length, salt) {
        this.length = length;
        this.salt = salt;
        this.name = "authenticationMD5Password";
      }
    };
    exports.AuthenticationMD5Password = AuthenticationMD5Password;
    var BackendKeyDataMessage = class {
      static {
        __name(this, "BackendKeyDataMessage");
      }
      static {
        __name2(this, "BackendKeyDataMessage");
      }
      constructor(length, processID, secretKey) {
        this.length = length;
        this.processID = processID;
        this.secretKey = secretKey;
        this.name = "backendKeyData";
      }
    };
    exports.BackendKeyDataMessage = BackendKeyDataMessage;
    var NotificationResponseMessage = class {
      static {
        __name(this, "NotificationResponseMessage");
      }
      static {
        __name2(this, "NotificationResponseMessage");
      }
      constructor(length, processId, channel, payload) {
        this.length = length;
        this.processId = processId;
        this.channel = channel;
        this.payload = payload;
        this.name = "notification";
      }
    };
    exports.NotificationResponseMessage = NotificationResponseMessage;
    var ReadyForQueryMessage = class {
      static {
        __name(this, "ReadyForQueryMessage");
      }
      static {
        __name2(this, "ReadyForQueryMessage");
      }
      constructor(length, status) {
        this.length = length;
        this.status = status;
        this.name = "readyForQuery";
      }
    };
    exports.ReadyForQueryMessage = ReadyForQueryMessage;
    var CommandCompleteMessage = class {
      static {
        __name(this, "CommandCompleteMessage");
      }
      static {
        __name2(this, "CommandCompleteMessage");
      }
      constructor(length, text) {
        this.length = length;
        this.text = text;
        this.name = "commandComplete";
      }
    };
    exports.CommandCompleteMessage = CommandCompleteMessage;
    var DataRowMessage = class {
      static {
        __name(this, "DataRowMessage");
      }
      static {
        __name2(this, "DataRowMessage");
      }
      constructor(length, fields) {
        this.length = length;
        this.fields = fields;
        this.name = "dataRow";
        this.fieldCount = fields.length;
      }
    };
    exports.DataRowMessage = DataRowMessage;
    var NoticeMessage = class {
      static {
        __name(this, "NoticeMessage");
      }
      static {
        __name2(this, "NoticeMessage");
      }
      constructor(length, message) {
        this.length = length;
        this.message = message;
        this.name = "notice";
      }
    };
    exports.NoticeMessage = NoticeMessage;
  }
});
var require_buffer_writer = __commonJS({
  "node_modules/pg-protocol/dist/buffer-writer.js"(exports) {
    "use strict";
    Object.defineProperty(exports, "__esModule", { value: true });
    exports.Writer = void 0;
    var Writer = class {
      static {
        __name(this, "Writer");
      }
      static {
        __name2(this, "Writer");
      }
      constructor(size = 256) {
        this.size = size;
        this.offset = 5;
        this.headerPosition = 0;
        this.buffer = Buffer.allocUnsafe(size);
      }
      ensure(size) {
        const remaining = this.buffer.length - this.offset;
        if (remaining < size) {
          const oldBuffer = this.buffer;
          const newSize = oldBuffer.length + (oldBuffer.length >> 1) + size;
          this.buffer = Buffer.allocUnsafe(newSize);
          oldBuffer.copy(this.buffer);
        }
      }
      addInt32(num) {
        this.ensure(4);
        this.buffer[this.offset++] = num >>> 24 & 255;
        this.buffer[this.offset++] = num >>> 16 & 255;
        this.buffer[this.offset++] = num >>> 8 & 255;
        this.buffer[this.offset++] = num >>> 0 & 255;
        return this;
      }
      addInt16(num) {
        this.ensure(2);
        this.buffer[this.offset++] = num >>> 8 & 255;
        this.buffer[this.offset++] = num >>> 0 & 255;
        return this;
      }
      addCString(string) {
        if (!string) {
          this.ensure(1);
        } else {
          const len = Buffer.byteLength(string);
          this.ensure(len + 1);
          this.buffer.write(string, this.offset, "utf-8");
          this.offset += len;
        }
        this.buffer[this.offset++] = 0;
        return this;
      }
      addString(string = "") {
        const len = Buffer.byteLength(string);
        this.ensure(len);
        this.buffer.write(string, this.offset);
        this.offset += len;
        return this;
      }
      // Write an Int32 byte-length prefix immediately followed by the string's UTF-8
      // bytes. Postgres' Bind wire format prefixes every parameter with its length,
      // and doing it in one method computes Buffer.byteLength ONCE — the previous
      // `addInt32(Buffer.byteLength(s)).addString(s)` pairing scanned the string
      // three times (byteLength for the prefix, byteLength again inside addString,
      // then the encode), which is costly for large text parameters.
      addInt32PrefixedString(string) {
        const len = Buffer.byteLength(string);
        this.ensure(4 + len);
        const buffer = this.buffer;
        let offset = this.offset;
        buffer[offset++] = len >>> 24 & 255;
        buffer[offset++] = len >>> 16 & 255;
        buffer[offset++] = len >>> 8 & 255;
        buffer[offset++] = len >>> 0 & 255;
        buffer.write(string, offset, "utf-8");
        this.offset = offset + len;
        return this;
      }
      add(otherBuffer) {
        this.ensure(otherBuffer.length);
        otherBuffer.copy(this.buffer, this.offset);
        this.offset += otherBuffer.length;
        return this;
      }
      join(code) {
        if (code) {
          this.buffer[this.headerPosition] = code;
          const length = this.offset - (this.headerPosition + 1);
          this.buffer.writeInt32BE(length, this.headerPosition + 1);
        }
        return this.buffer.slice(code ? 0 : 5, this.offset);
      }
      flush(code) {
        const result = this.join(code);
        this.offset = 5;
        this.headerPosition = 0;
        this.buffer = Buffer.allocUnsafe(this.size);
        return result;
      }
      clear() {
        this.offset = 5;
        this.headerPosition = 0;
      }
    };
    exports.Writer = Writer;
  }
});
var require_serializer = __commonJS({
  "node_modules/pg-protocol/dist/serializer.js"(exports) {
    "use strict";
    Object.defineProperty(exports, "__esModule", { value: true });
    exports.serialize = void 0;
    var buffer_writer_1 = require_buffer_writer();
    var writer = new buffer_writer_1.Writer();
    var startup = /* @__PURE__ */ __name2((opts) => {
      writer.addInt16(3).addInt16(0);
      for (const key of Object.keys(opts)) {
        writer.addCString(key).addCString(opts[key]);
      }
      writer.addCString("client_encoding").addCString("UTF8");
      const bodyBuffer = writer.addCString("").flush();
      const length = bodyBuffer.length + 4;
      return new buffer_writer_1.Writer().addInt32(length).add(bodyBuffer).flush();
    }, "startup");
    var requestSsl = /* @__PURE__ */ __name2(() => {
      const response = Buffer.allocUnsafe(8);
      response.writeInt32BE(8, 0);
      response.writeInt32BE(80877103, 4);
      return response;
    }, "requestSsl");
    var password = /* @__PURE__ */ __name2((password2) => {
      return writer.addCString(password2).flush(
        112
        /* code.startup */
      );
    }, "password");
    var sendSASLInitialResponseMessage = /* @__PURE__ */ __name2(function(mechanism, initialResponse) {
      writer.addCString(mechanism).addInt32PrefixedString(initialResponse);
      return writer.flush(
        112
        /* code.startup */
      );
    }, "sendSASLInitialResponseMessage");
    var sendSCRAMClientFinalMessage = /* @__PURE__ */ __name2(function(additionalData) {
      return writer.addString(additionalData).flush(
        112
        /* code.startup */
      );
    }, "sendSCRAMClientFinalMessage");
    var query = /* @__PURE__ */ __name2((text) => {
      return writer.addCString(text).flush(
        81
        /* code.query */
      );
    }, "query");
    var emptyArray = [];
    var parse = /* @__PURE__ */ __name2((query2) => {
      const name = query2.name || "";
      if (name.length > 63) {
        console.error("Warning! Postgres only supports 63 characters for query names.");
        console.error("You supplied %s (%s)", name, name.length);
        console.error("This can cause conflicts and silent errors executing queries");
      }
      const types2 = query2.types || emptyArray;
      const len = types2.length;
      const buffer = writer.addCString(name).addCString(query2.text).addInt16(len);
      for (let i = 0; i < len; i++) {
        buffer.addInt32(types2[i]);
      }
      return writer.flush(
        80
        /* code.parse */
      );
    }, "parse");
    var paramWriter = new buffer_writer_1.Writer();
    var writeValues = /* @__PURE__ */ __name2(function(values, valueMapper) {
      for (let i = 0; i < values.length; i++) {
        const mappedVal = valueMapper ? valueMapper(values[i], i) : values[i];
        if (mappedVal == null) {
          writer.addInt16(
            0
            /* ParamType.STRING */
          );
          paramWriter.addInt32(-1);
        } else if (mappedVal instanceof Buffer) {
          writer.addInt16(
            1
            /* ParamType.BINARY */
          );
          paramWriter.addInt32(mappedVal.length);
          paramWriter.add(mappedVal);
        } else {
          writer.addInt16(
            0
            /* ParamType.STRING */
          );
          paramWriter.addInt32PrefixedString(mappedVal);
        }
      }
    }, "writeValues");
    var bind = /* @__PURE__ */ __name2((config = {}) => {
      const portal = config.portal || "";
      const statement = config.statement || "";
      const binary = config.binary || false;
      const values = config.values || emptyArray;
      const len = values.length;
      writer.addCString(portal).addCString(statement);
      writer.addInt16(len);
      try {
        writeValues(values, config.valueMapper);
      } catch (err) {
        writer.clear();
        paramWriter.clear();
        throw err;
      }
      writer.addInt16(len);
      writer.add(paramWriter.flush());
      writer.addInt16(1);
      writer.addInt16(
        binary ? 1 : 0
        /* ParamType.STRING */
      );
      return writer.flush(
        66
        /* code.bind */
      );
    }, "bind");
    var emptyExecute = Buffer.from([69, 0, 0, 0, 9, 0, 0, 0, 0, 0]);
    var execute = /* @__PURE__ */ __name2((config) => {
      if (!config || !config.portal && !config.rows) {
        return emptyExecute;
      }
      const portal = config.portal || "";
      const rows = config.rows || 0;
      const portalLength = Buffer.byteLength(portal);
      const len = 4 + portalLength + 1 + 4;
      const buff = Buffer.allocUnsafe(1 + len);
      buff[0] = 69;
      buff.writeInt32BE(len, 1);
      buff.write(portal, 5, "utf-8");
      buff[portalLength + 5] = 0;
      buff.writeUInt32BE(rows, buff.length - 4);
      return buff;
    }, "execute");
    var cancel = /* @__PURE__ */ __name2((processID, secretKey) => {
      const buffer = Buffer.allocUnsafe(16);
      buffer.writeInt32BE(16, 0);
      buffer.writeInt16BE(1234, 4);
      buffer.writeInt16BE(5678, 6);
      buffer.writeInt32BE(processID, 8);
      buffer.writeInt32BE(secretKey, 12);
      return buffer;
    }, "cancel");
    var cstringMessage = /* @__PURE__ */ __name2((code, string) => {
      const stringLen = Buffer.byteLength(string);
      const len = 4 + stringLen + 1;
      const buffer = Buffer.allocUnsafe(1 + len);
      buffer[0] = code;
      buffer.writeInt32BE(len, 1);
      buffer.write(string, 5, "utf-8");
      buffer[len] = 0;
      return buffer;
    }, "cstringMessage");
    var emptyDescribePortal = writer.addCString("P").flush(
      68
      /* code.describe */
    );
    var emptyDescribeStatement = writer.addCString("S").flush(
      68
      /* code.describe */
    );
    var describe = /* @__PURE__ */ __name2((msg) => {
      return msg.name ? cstringMessage(68, `${msg.type}${msg.name || ""}`) : msg.type === "P" ? emptyDescribePortal : emptyDescribeStatement;
    }, "describe");
    var close = /* @__PURE__ */ __name2((msg) => {
      const text = `${msg.type}${msg.name || ""}`;
      return cstringMessage(67, text);
    }, "close");
    var copyData = /* @__PURE__ */ __name2((chunk) => {
      return writer.add(chunk).flush(
        100
        /* code.copyFromChunk */
      );
    }, "copyData");
    var copyFail = /* @__PURE__ */ __name2((message) => {
      return cstringMessage(102, message);
    }, "copyFail");
    var codeOnlyBuffer = /* @__PURE__ */ __name2((code) => Buffer.from([code, 0, 0, 0, 4]), "codeOnlyBuffer");
    var flushBuffer = codeOnlyBuffer(
      72
      /* code.flush */
    );
    var syncBuffer = codeOnlyBuffer(
      83
      /* code.sync */
    );
    var endBuffer = codeOnlyBuffer(
      88
      /* code.end */
    );
    var copyDoneBuffer = codeOnlyBuffer(
      99
      /* code.copyDone */
    );
    var serialize = {
      startup,
      password,
      requestSsl,
      sendSASLInitialResponseMessage,
      sendSCRAMClientFinalMessage,
      query,
      parse,
      bind,
      execute,
      describe,
      close,
      flush: /* @__PURE__ */ __name2(() => flushBuffer, "flush"),
      sync: /* @__PURE__ */ __name2(() => syncBuffer, "sync"),
      end: /* @__PURE__ */ __name2(() => endBuffer, "end"),
      copyData,
      copyDone: /* @__PURE__ */ __name2(() => copyDoneBuffer, "copyDone"),
      copyFail,
      cancel
    };
    exports.serialize = serialize;
  }
});
var require_buffer_reader = __commonJS({
  "node_modules/pg-protocol/dist/buffer-reader.js"(exports) {
    "use strict";
    Object.defineProperty(exports, "__esModule", { value: true });
    exports.BufferReader = void 0;
    var BufferReader = class {
      static {
        __name(this, "BufferReader");
      }
      static {
        __name2(this, "BufferReader");
      }
      constructor(offset = 0) {
        this.offset = offset;
        this.buffer = Buffer.allocUnsafe(0);
        this.encoding = "utf-8";
      }
      setBuffer(offset, buffer) {
        this.offset = offset;
        this.buffer = buffer;
      }
      int16() {
        const result = this.buffer.readInt16BE(this.offset);
        this.offset += 2;
        return result;
      }
      byte() {
        const result = this.buffer[this.offset];
        this.offset++;
        return result;
      }
      int32() {
        const result = this.buffer.readInt32BE(this.offset);
        this.offset += 4;
        return result;
      }
      uint32() {
        const result = this.buffer.readUInt32BE(this.offset);
        this.offset += 4;
        return result;
      }
      string(length) {
        const result = this.buffer.toString(this.encoding, this.offset, this.offset + length);
        this.offset += length;
        return result;
      }
      cstring() {
        const start = this.offset;
        let end = start;
        while (this.buffer[end++]) {
        }
        this.offset = end;
        return this.buffer.toString(this.encoding, start, end - 1);
      }
      bytes(length) {
        const result = this.buffer.slice(this.offset, this.offset + length);
        this.offset += length;
        return result;
      }
    };
    exports.BufferReader = BufferReader;
  }
});
var require_parser = __commonJS({
  "node_modules/pg-protocol/dist/parser.js"(exports) {
    "use strict";
    Object.defineProperty(exports, "__esModule", { value: true });
    exports.Parser = void 0;
    var messages_1 = require_messages();
    var buffer_reader_1 = require_buffer_reader();
    var CODE_LENGTH = 1;
    var LEN_LENGTH = 4;
    var HEADER_LENGTH = CODE_LENGTH + LEN_LENGTH;
    var LATEINIT_LENGTH = -1;
    var emptyBuffer = Buffer.allocUnsafe(0);
    var Parser = class {
      static {
        __name(this, "Parser");
      }
      static {
        __name2(this, "Parser");
      }
      constructor(opts) {
        this.buffer = emptyBuffer;
        this.bufferLength = 0;
        this.bufferOffset = 0;
        this.reader = new buffer_reader_1.BufferReader();
        if ((opts === null || opts === void 0 ? void 0 : opts.mode) === "binary") {
          throw new Error("Binary mode not supported yet");
        }
        this.mode = (opts === null || opts === void 0 ? void 0 : opts.mode) || "text";
      }
      parse(buffer, callback) {
        this.mergeBuffer(buffer);
        const bufferFullLength = this.bufferOffset + this.bufferLength;
        let offset = this.bufferOffset;
        while (offset + HEADER_LENGTH <= bufferFullLength) {
          const code = this.buffer[offset];
          const length = this.buffer.readUInt32BE(offset + CODE_LENGTH);
          const fullMessageLength = CODE_LENGTH + length;
          if (fullMessageLength + offset <= bufferFullLength) {
            const message = this.handlePacket(offset + HEADER_LENGTH, code, length, this.buffer);
            callback(message);
            offset += fullMessageLength;
          } else {
            break;
          }
        }
        if (offset === bufferFullLength) {
          this.buffer = emptyBuffer;
          this.bufferLength = 0;
          this.bufferOffset = 0;
        } else {
          this.bufferLength = bufferFullLength - offset;
          this.bufferOffset = offset;
        }
      }
      mergeBuffer(buffer) {
        if (this.bufferLength > 0) {
          const newLength = this.bufferLength + buffer.byteLength;
          const newFullLength = newLength + this.bufferOffset;
          if (newFullLength > this.buffer.byteLength) {
            let newBuffer;
            if (newLength <= this.buffer.byteLength && this.bufferOffset >= this.bufferLength) {
              newBuffer = this.buffer;
            } else {
              let newBufferLength = this.buffer.byteLength * 2;
              while (newLength >= newBufferLength) {
                newBufferLength *= 2;
              }
              newBuffer = Buffer.allocUnsafe(newBufferLength);
            }
            this.buffer.copy(newBuffer, 0, this.bufferOffset, this.bufferOffset + this.bufferLength);
            this.buffer = newBuffer;
            this.bufferOffset = 0;
          }
          buffer.copy(this.buffer, this.bufferOffset + this.bufferLength);
          this.bufferLength = newLength;
        } else {
          this.buffer = buffer;
          this.bufferOffset = 0;
          this.bufferLength = buffer.byteLength;
        }
      }
      handlePacket(offset, code, length, bytes) {
        const { reader } = this;
        reader.setBuffer(offset, bytes);
        let message;
        switch (code) {
          case 50:
            message = messages_1.bindComplete;
            break;
          case 49:
            message = messages_1.parseComplete;
            break;
          case 51:
            message = messages_1.closeComplete;
            break;
          case 110:
            message = messages_1.noData;
            break;
          case 115:
            message = messages_1.portalSuspended;
            break;
          case 99:
            message = messages_1.copyDone;
            break;
          case 87:
            message = messages_1.replicationStart;
            break;
          case 73:
            message = messages_1.emptyQuery;
            break;
          case 68:
            message = parseDataRowMessage(reader);
            break;
          case 67:
            message = parseCommandCompleteMessage(reader);
            break;
          case 90:
            message = parseReadyForQueryMessage(reader);
            break;
          case 65:
            message = parseNotificationMessage(reader);
            break;
          case 82:
            message = parseAuthenticationResponse(reader, length);
            break;
          case 83:
            message = parseParameterStatusMessage(reader);
            break;
          case 75:
            message = parseBackendKeyData(reader);
            break;
          case 69:
            message = parseErrorMessage(reader, "error");
            break;
          case 78:
            message = parseErrorMessage(reader, "notice");
            break;
          case 84:
            message = parseRowDescriptionMessage(reader);
            break;
          case 116:
            message = parseParameterDescriptionMessage(reader);
            break;
          case 71:
            message = parseCopyInMessage(reader);
            break;
          case 72:
            message = parseCopyOutMessage(reader);
            break;
          case 100:
            message = parseCopyData(reader, length);
            break;
          default:
            return new messages_1.DatabaseError("received invalid response: " + code.toString(16), length, "error");
        }
        reader.setBuffer(0, emptyBuffer);
        message.length = length;
        return message;
      }
    };
    exports.Parser = Parser;
    var parseReadyForQueryMessage = /* @__PURE__ */ __name2((reader) => {
      const status = reader.string(1);
      return new messages_1.ReadyForQueryMessage(LATEINIT_LENGTH, status);
    }, "parseReadyForQueryMessage");
    var parseCommandCompleteMessage = /* @__PURE__ */ __name2((reader) => {
      const text = reader.cstring();
      return new messages_1.CommandCompleteMessage(LATEINIT_LENGTH, text);
    }, "parseCommandCompleteMessage");
    var parseCopyData = /* @__PURE__ */ __name2((reader, length) => {
      const chunk = reader.bytes(length - 4);
      return new messages_1.CopyDataMessage(LATEINIT_LENGTH, chunk);
    }, "parseCopyData");
    var parseCopyInMessage = /* @__PURE__ */ __name2((reader) => parseCopyMessage(reader, "copyInResponse"), "parseCopyInMessage");
    var parseCopyOutMessage = /* @__PURE__ */ __name2((reader) => parseCopyMessage(reader, "copyOutResponse"), "parseCopyOutMessage");
    var parseCopyMessage = /* @__PURE__ */ __name2((reader, messageName) => {
      const isBinary = reader.byte() !== 0;
      const columnCount = reader.int16();
      const message = new messages_1.CopyResponse(LATEINIT_LENGTH, messageName, isBinary, columnCount);
      for (let i = 0; i < columnCount; i++) {
        message.columnTypes[i] = reader.int16();
      }
      return message;
    }, "parseCopyMessage");
    var parseNotificationMessage = /* @__PURE__ */ __name2((reader) => {
      const processId = reader.int32();
      const channel = reader.cstring();
      const payload = reader.cstring();
      return new messages_1.NotificationResponseMessage(LATEINIT_LENGTH, processId, channel, payload);
    }, "parseNotificationMessage");
    var parseRowDescriptionMessage = /* @__PURE__ */ __name2((reader) => {
      const fieldCount = reader.int16();
      const message = new messages_1.RowDescriptionMessage(LATEINIT_LENGTH, fieldCount);
      for (let i = 0; i < fieldCount; i++) {
        message.fields[i] = parseField(reader);
      }
      return message;
    }, "parseRowDescriptionMessage");
    var parseField = /* @__PURE__ */ __name2((reader) => {
      const name = reader.cstring();
      const tableID = reader.uint32();
      const columnID = reader.int16();
      const dataTypeID = reader.uint32();
      const dataTypeSize = reader.int16();
      const dataTypeModifier = reader.int32();
      const mode = reader.int16() === 0 ? "text" : "binary";
      return new messages_1.Field(name, tableID, columnID, dataTypeID, dataTypeSize, dataTypeModifier, mode);
    }, "parseField");
    var parseParameterDescriptionMessage = /* @__PURE__ */ __name2((reader) => {
      const parameterCount = reader.int16();
      const message = new messages_1.ParameterDescriptionMessage(LATEINIT_LENGTH, parameterCount);
      for (let i = 0; i < parameterCount; i++) {
        message.dataTypeIDs[i] = reader.int32();
      }
      return message;
    }, "parseParameterDescriptionMessage");
    var parseDataRowMessage = /* @__PURE__ */ __name2((reader) => {
      const fieldCount = reader.int16();
      const fields = new Array(fieldCount);
      for (let i = 0; i < fieldCount; i++) {
        const len = reader.int32();
        fields[i] = len === -1 ? null : reader.string(len);
      }
      return new messages_1.DataRowMessage(LATEINIT_LENGTH, fields);
    }, "parseDataRowMessage");
    var parseParameterStatusMessage = /* @__PURE__ */ __name2((reader) => {
      const name = reader.cstring();
      const value = reader.cstring();
      return new messages_1.ParameterStatusMessage(LATEINIT_LENGTH, name, value);
    }, "parseParameterStatusMessage");
    var parseBackendKeyData = /* @__PURE__ */ __name2((reader) => {
      const processID = reader.int32();
      const secretKey = reader.int32();
      return new messages_1.BackendKeyDataMessage(LATEINIT_LENGTH, processID, secretKey);
    }, "parseBackendKeyData");
    var parseAuthenticationResponse = /* @__PURE__ */ __name2((reader, length) => {
      const code = reader.int32();
      const message = {
        name: "authenticationOk",
        length
      };
      switch (code) {
        case 0:
          break;
        case 3:
          if (message.length === 8) {
            message.name = "authenticationCleartextPassword";
          }
          break;
        case 5:
          if (message.length === 12) {
            message.name = "authenticationMD5Password";
            const salt = reader.bytes(4);
            return new messages_1.AuthenticationMD5Password(LATEINIT_LENGTH, salt);
          }
          break;
        case 10:
          {
            message.name = "authenticationSASL";
            message.mechanisms = [];
            let mechanism;
            do {
              mechanism = reader.cstring();
              if (mechanism) {
                message.mechanisms.push(mechanism);
              }
            } while (mechanism);
          }
          break;
        case 11:
          message.name = "authenticationSASLContinue";
          message.data = reader.string(length - 8);
          break;
        case 12:
          message.name = "authenticationSASLFinal";
          message.data = reader.string(length - 8);
          break;
        default:
          throw new Error("Unknown authenticationOk message type " + code);
      }
      return message;
    }, "parseAuthenticationResponse");
    var parseErrorMessage = /* @__PURE__ */ __name2((reader, name) => {
      const fields = {};
      let fieldType = reader.string(1);
      while (fieldType !== "\0") {
        fields[fieldType] = reader.cstring();
        fieldType = reader.string(1);
      }
      const messageValue = fields.M;
      const message = name === "notice" ? new messages_1.NoticeMessage(LATEINIT_LENGTH, messageValue) : new messages_1.DatabaseError(messageValue, LATEINIT_LENGTH, name);
      message.severity = fields.S;
      message.code = fields.C;
      message.detail = fields.D;
      message.hint = fields.H;
      message.position = fields.P;
      message.internalPosition = fields.p;
      message.internalQuery = fields.q;
      message.where = fields.W;
      message.schema = fields.s;
      message.table = fields.t;
      message.column = fields.c;
      message.dataType = fields.d;
      message.constraint = fields.n;
      message.file = fields.F;
      message.line = fields.L;
      message.routine = fields.R;
      return message;
    }, "parseErrorMessage");
  }
});
var require_dist = __commonJS({
  "node_modules/pg-protocol/dist/index.js"(exports) {
    "use strict";
    Object.defineProperty(exports, "__esModule", { value: true });
    exports.DatabaseError = exports.serialize = void 0;
    exports.parse = parse;
    var messages_1 = require_messages();
    Object.defineProperty(exports, "DatabaseError", { enumerable: true, get: /* @__PURE__ */ __name2(function() {
      return messages_1.DatabaseError;
    }, "get") });
    var serializer_1 = require_serializer();
    Object.defineProperty(exports, "serialize", { enumerable: true, get: /* @__PURE__ */ __name2(function() {
      return serializer_1.serialize;
    }, "get") });
    var parser_1 = require_parser();
    function parse(stream, callback) {
      const parser = new parser_1.Parser();
      stream.on("data", (buffer) => parser.parse(buffer, callback));
      return new Promise((resolve) => stream.on("end", () => resolve()));
    }
    __name(parse, "parse");
    __name2(parse, "parse");
  }
});
var require_net = __commonJS({
  "node-built-in-modules:net"(exports, module) {
    module.exports = libDefault7;
  }
});
var require_tls = __commonJS({
  "node-built-in-modules:tls"(exports, module) {
    module.exports = libDefault8;
  }
});
var require_dist2 = __commonJS({
  "node_modules/pg-cloudflare/dist/index.js"(exports) {
    "use strict";
    Object.defineProperty(exports, "__esModule", { value: true });
    exports.CloudflareSocket = void 0;
    var events_1 = require_events();
    var CloudflareSocket = class extends events_1.EventEmitter {
      static {
        __name(this, "CloudflareSocket");
      }
      static {
        __name2(this, "CloudflareSocket");
      }
      constructor(ssl) {
        super();
        this.ssl = ssl;
        this.writable = false;
        this.destroyed = false;
        this._upgrading = false;
        this._upgraded = false;
        this._cfSocket = null;
        this._cfWriter = null;
        this._cfReader = null;
      }
      setNoDelay() {
        return this;
      }
      setKeepAlive() {
        return this;
      }
      ref() {
        return this;
      }
      unref() {
        return this;
      }
      async connect(port, host, connectListener) {
        try {
          log("connecting");
          if (connectListener)
            this.once("connect", connectListener);
          const options = this.ssl ? { secureTransport: "starttls" } : {};
          const mod = await import("cloudflare:sockets");
          const connect = mod.connect;
          this._cfSocket = connect(`${host}:${port}`, options);
          this._cfWriter = this._cfSocket.writable.getWriter();
          this._addClosedHandler();
          this._cfReader = this._cfSocket.readable.getReader();
          if (this.ssl) {
            this._listenOnce().catch((e) => this.emit("error", e));
          } else {
            this._listen().catch((e) => this.emit("error", e));
          }
          await this._cfWriter.ready;
          log("socket ready");
          this.writable = true;
          this.emit("connect");
          return this;
        } catch (e) {
          this.emit("error", e);
        }
      }
      async _listen() {
        while (true) {
          log("awaiting receive from CF socket");
          const { done, value } = await this._cfReader.read();
          log("CF socket received:", done, value);
          if (done) {
            log("done");
            break;
          }
          this.emit("data", Buffer.from(value));
        }
      }
      async _listenOnce() {
        log("awaiting first receive from CF socket");
        const { done, value } = await this._cfReader.read();
        log("First CF socket received:", done, value);
        this.emit("data", Buffer.from(value));
      }
      write(data, encoding = "utf8", callback = () => {
      }) {
        if (data.length === 0)
          return callback();
        if (typeof data === "string")
          data = Buffer.from(data, encoding);
        log("sending data direct:", data);
        this._cfWriter.write(data).then(() => {
          log("data sent");
          callback();
        }, (err) => {
          log("send error", err);
          callback(err);
        });
        return true;
      }
      end(data = Buffer.alloc(0), encoding = "utf8", callback = () => {
      }) {
        log("ending CF socket");
        this.write(data, encoding, (err) => {
          this._cfSocket.close();
          if (callback)
            callback(err);
        });
        return this;
      }
      destroy(reason) {
        log("destroying CF socket", reason);
        this.destroyed = true;
        return this.end();
      }
      startTls(options) {
        if (this._upgraded) {
          this.emit("error", "Cannot call `startTls()` more than once on a socket");
          return;
        }
        this._cfWriter.releaseLock();
        this._cfReader.releaseLock();
        this._upgrading = true;
        this._cfSocket = this._cfSocket.startTls(options);
        this._cfWriter = this._cfSocket.writable.getWriter();
        this._cfReader = this._cfSocket.readable.getReader();
        this._addClosedHandler();
        this._listen().catch((e) => this.emit("error", e));
      }
      _addClosedHandler() {
        this._cfSocket.closed.then(() => {
          if (!this._upgrading) {
            log("CF socket closed");
            this._cfSocket = null;
            this.emit("close");
          } else {
            this._upgrading = false;
            this._upgraded = true;
          }
        }).catch((e) => this.emit("error", e));
      }
    };
    exports.CloudflareSocket = CloudflareSocket;
    var debug = false;
    function dump(data) {
      if (data instanceof Uint8Array || data instanceof ArrayBuffer) {
        const buf = data instanceof Uint8Array ? Buffer.from(data) : Buffer.from(data);
        const hex = buf.toString("hex");
        const str = new TextDecoder().decode(data);
        return `
>>> STR: "${str.replace(/\n/g, "\\n")}"
>>> HEX: ${hex}
`;
      } else {
        return data;
      }
    }
    __name(dump, "dump");
    __name2(dump, "dump");
    function log(...args) {
      debug && console.log(...args.map(dump));
    }
    __name(log, "log");
    __name2(log, "log");
  }
});
var require_stream = __commonJS({
  "node_modules/pg/lib/stream.js"(exports, module) {
    var { getStream, getSecureStream } = getStreamFuncs();
    module.exports = {
      /**
       * Get a socket stream compatible with the current runtime environment.
       * @returns {Duplex}
       */
      getStream,
      /**
       * Get a TLS secured socket, compatible with the current environment,
       * using the socket and other settings given in `options`.
       * @returns {Duplex}
       */
      getSecureStream
    };
    function getNodejsStreamFuncs() {
      function getStream2(ssl) {
        const net = require_net();
        return new net.Socket();
      }
      __name(getStream2, "getStream2");
      __name2(getStream2, "getStream");
      function getSecureStream2(options) {
        const tls = require_tls();
        return tls.connect(options);
      }
      __name(getSecureStream2, "getSecureStream2");
      __name2(getSecureStream2, "getSecureStream");
      return {
        getStream: getStream2,
        getSecureStream: getSecureStream2
      };
    }
    __name(getNodejsStreamFuncs, "getNodejsStreamFuncs");
    __name2(getNodejsStreamFuncs, "getNodejsStreamFuncs");
    function getCloudflareStreamFuncs() {
      function getStream2(ssl) {
        const { CloudflareSocket } = require_dist2();
        return new CloudflareSocket(ssl);
      }
      __name(getStream2, "getStream2");
      __name2(getStream2, "getStream");
      function getSecureStream2(options) {
        options.socket.startTls(options);
        return options.socket;
      }
      __name(getSecureStream2, "getSecureStream2");
      __name2(getSecureStream2, "getSecureStream");
      return {
        getStream: getStream2,
        getSecureStream: getSecureStream2
      };
    }
    __name(getCloudflareStreamFuncs, "getCloudflareStreamFuncs");
    __name2(getCloudflareStreamFuncs, "getCloudflareStreamFuncs");
    function isCloudflareRuntime() {
      if (typeof navigator === "object" && navigator !== null && true) {
        return true;
      }
      if (typeof Response === "function") {
        const resp = new Response(null, { cf: { thing: true } });
        if (typeof resp.cf === "object" && resp.cf !== null && resp.cf.thing) {
          return true;
        }
      }
      return false;
    }
    __name(isCloudflareRuntime, "isCloudflareRuntime");
    __name2(isCloudflareRuntime, "isCloudflareRuntime");
    function getStreamFuncs() {
      if (isCloudflareRuntime()) {
        return getCloudflareStreamFuncs();
      }
      return getNodejsStreamFuncs();
    }
    __name(getStreamFuncs, "getStreamFuncs");
    __name2(getStreamFuncs, "getStreamFuncs");
  }
});
var require_connection = __commonJS({
  "node_modules/pg/lib/connection.js"(exports, module) {
    "use strict";
    var EventEmitter = require_events().EventEmitter;
    var { parse, serialize } = require_dist();
    var stream = require_stream();
    var { getStream } = stream;
    var flushBuffer = serialize.flush();
    var syncBuffer = serialize.sync();
    var endBuffer = serialize.end();
    var Connection2 = class extends EventEmitter {
      static {
        __name(this, "Connection2");
      }
      static {
        __name2(this, "Connection");
      }
      constructor(config) {
        super();
        config = config || {};
        this.stream = config.stream || getStream(config.ssl);
        if (typeof this.stream === "function") {
          this.stream = this.stream(config);
        }
        this._keepAlive = config.keepAlive;
        this._keepAliveInitialDelayMillis = config.keepAliveInitialDelayMillis;
        this.parsedStatements = {};
        this.ssl = config.ssl || false;
        this.sslNegotiation = config.sslNegotiation || "postgres";
        this._ending = false;
        this._emitMessage = false;
        const self = this;
        this.on("newListener", function(eventName) {
          if (eventName === "message") {
            self._emitMessage = true;
          }
        });
      }
      connect(port, host) {
        const self = this;
        this._connecting = true;
        this.stream.setNoDelay(true);
        this.stream.connect(port, host);
        this.stream.once("connect", function() {
          if (self._keepAlive) {
            self.stream.setKeepAlive(true, self._keepAliveInitialDelayMillis);
          }
          self.emit("connect");
        });
        const reportStreamError = /* @__PURE__ */ __name2(function(error) {
          if (self._ending && (error.code === "ECONNRESET" || error.code === "EPIPE")) {
            return;
          }
          self.emit("error", error);
        }, "reportStreamError");
        this.stream.on("error", reportStreamError);
        this.stream.on("close", function() {
          self.emit("end");
        });
        if (!this.ssl) {
          return this.attachListeners(this.stream);
        }
        if (this.sslNegotiation === "direct") {
          return this.stream.once("connect", function() {
            self.upgradeToSSL(host, reportStreamError);
          });
        }
        this.stream.once("data", function(buffer) {
          const responseCode = buffer.toString("utf8");
          switch (responseCode) {
            case "S":
              break;
            case "N":
              self.stream.end();
              return self.emit("error", new Error("The server does not support SSL connections"));
            default:
              self.stream.end();
              return self.emit("error", new Error("There was an error establishing an SSL connection"));
          }
          self.upgradeToSSL(host, reportStreamError);
        });
      }
      upgradeToSSL(host, reportStreamError) {
        const self = this;
        const options = {
          socket: self.stream
        };
        if (self.ssl !== true) {
          Object.assign(options, self.ssl);
          if ("key" in self.ssl) {
            options.key = self.ssl.key;
          }
        }
        if (self.sslNegotiation === "direct") {
          options.ALPNProtocols = ["postgresql"];
        }
        const net = require_net();
        if (net.isIP && net.isIP(host) === 0) {
          options.servername = host;
        }
        try {
          self.stream = stream.getSecureStream(options);
        } catch (err) {
          return self.emit("error", err);
        }
        self.attachListeners(self.stream);
        self.stream.on("error", reportStreamError);
        self.emit("sslconnect");
      }
      attachListeners(stream2) {
        parse(stream2, (msg) => {
          const eventName = msg.name === "error" ? "errorMessage" : msg.name;
          if (this._emitMessage) {
            this.emit("message", msg);
          }
          this.emit(eventName, msg);
        });
      }
      requestSsl() {
        this.stream.write(serialize.requestSsl());
      }
      startup(config) {
        this.stream.write(serialize.startup(config));
      }
      cancel(processID, secretKey) {
        this._send(serialize.cancel(processID, secretKey));
      }
      password(password) {
        this._send(serialize.password(password));
      }
      sendSASLInitialResponseMessage(mechanism, initialResponse) {
        this._send(serialize.sendSASLInitialResponseMessage(mechanism, initialResponse));
      }
      sendSCRAMClientFinalMessage(additionalData) {
        this._send(serialize.sendSCRAMClientFinalMessage(additionalData));
      }
      _send(buffer) {
        if (!this.stream.writable) {
          return false;
        }
        return this.stream.write(buffer);
      }
      query(text) {
        this._send(serialize.query(text));
      }
      // send parse message
      parse(query) {
        this._send(serialize.parse(query));
      }
      // send bind message
      bind(config) {
        this._send(serialize.bind(config));
      }
      // send execute message
      execute(config) {
        this._send(serialize.execute(config));
      }
      flush() {
        if (this.stream.writable) {
          this.stream.write(flushBuffer);
        }
      }
      sync() {
        this._ending = true;
        this._send(syncBuffer);
      }
      ref() {
        this.stream.ref();
      }
      unref() {
        this.stream.unref();
      }
      end() {
        this._ending = true;
        if (!this._connecting || !this.stream.writable) {
          this.stream.end();
          return;
        }
        return this.stream.write(endBuffer, () => {
          this.stream.end();
        });
      }
      close(msg) {
        this._send(serialize.close(msg));
      }
      describe(msg) {
        this._send(serialize.describe(msg));
      }
      sendCopyFromChunk(chunk) {
        this._send(serialize.copyData(chunk));
      }
      endCopyFrom() {
        this._send(serialize.copyDone());
      }
      sendCopyFail(msg) {
        this._send(serialize.copyFail(msg));
      }
    };
    module.exports = Connection2;
  }
});
var require_path = __commonJS({
  "node-built-in-modules:path"(exports, module) {
    module.exports = libDefault9;
  }
});
var require_stream2 = __commonJS({
  "node-built-in-modules:stream"(exports, module) {
    module.exports = libDefault10;
  }
});
var require_string_decoder = __commonJS({
  "node-built-in-modules:string_decoder"(exports, module) {
    module.exports = libDefault11;
  }
});
var require_split2 = __commonJS({
  "node_modules/split2/index.js"(exports, module) {
    "use strict";
    var { Transform } = require_stream2();
    var { StringDecoder } = require_string_decoder();
    var kLast = /* @__PURE__ */ Symbol("last");
    var kDecoder = /* @__PURE__ */ Symbol("decoder");
    function transform(chunk, enc, cb) {
      let list;
      if (this.overflow) {
        const buf = this[kDecoder].write(chunk);
        list = buf.split(this.matcher);
        if (list.length === 1) return cb();
        list.shift();
        this.overflow = false;
      } else {
        this[kLast] += this[kDecoder].write(chunk);
        list = this[kLast].split(this.matcher);
      }
      this[kLast] = list.pop();
      for (let i = 0; i < list.length; i++) {
        try {
          push(this, this.mapper(list[i]));
        } catch (error) {
          return cb(error);
        }
      }
      this.overflow = this[kLast].length > this.maxLength;
      if (this.overflow && !this.skipOverflow) {
        cb(new Error("maximum buffer reached"));
        return;
      }
      cb();
    }
    __name(transform, "transform");
    __name2(transform, "transform");
    function flush(cb) {
      this[kLast] += this[kDecoder].end();
      if (this[kLast]) {
        try {
          push(this, this.mapper(this[kLast]));
        } catch (error) {
          return cb(error);
        }
      }
      cb();
    }
    __name(flush, "flush");
    __name2(flush, "flush");
    function push(self, val) {
      if (val !== void 0) {
        self.push(val);
      }
    }
    __name(push, "push");
    __name2(push, "push");
    function noop(incoming) {
      return incoming;
    }
    __name(noop, "noop");
    __name2(noop, "noop");
    function split(matcher, mapper, options) {
      matcher = matcher || /\r?\n/;
      mapper = mapper || noop;
      options = options || {};
      switch (arguments.length) {
        case 1:
          if (typeof matcher === "function") {
            mapper = matcher;
            matcher = /\r?\n/;
          } else if (typeof matcher === "object" && !(matcher instanceof RegExp) && !matcher[Symbol.split]) {
            options = matcher;
            matcher = /\r?\n/;
          }
          break;
        case 2:
          if (typeof matcher === "function") {
            options = mapper;
            mapper = matcher;
            matcher = /\r?\n/;
          } else if (typeof mapper === "object") {
            options = mapper;
            mapper = noop;
          }
      }
      options = Object.assign({}, options);
      options.autoDestroy = true;
      options.transform = transform;
      options.flush = flush;
      options.readableObjectMode = true;
      const stream = new Transform(options);
      stream[kLast] = "";
      stream[kDecoder] = new StringDecoder("utf8");
      stream.matcher = matcher;
      stream.mapper = mapper;
      stream.maxLength = options.maxLength;
      stream.skipOverflow = options.skipOverflow || false;
      stream.overflow = false;
      stream._destroy = function(err, cb) {
        this._writableState.errorEmitted = false;
        cb(err);
      };
      return stream;
    }
    __name(split, "split");
    __name2(split, "split");
    module.exports = split;
  }
});
var require_helper = __commonJS({
  "node_modules/pgpass/lib/helper.js"(exports, module) {
    "use strict";
    var path = require_path();
    var Stream = require_stream2().Stream;
    var split = require_split2();
    var util = require_util();
    var defaultPort = 5432;
    var isWin = process.platform === "win32";
    var warnStream = process.stderr;
    var S_IRWXG = 56;
    var S_IRWXO = 7;
    var S_IFMT = 61440;
    var S_IFREG = 32768;
    function isRegFile(mode) {
      return (mode & S_IFMT) == S_IFREG;
    }
    __name(isRegFile, "isRegFile");
    __name2(isRegFile, "isRegFile");
    var fieldNames = ["host", "port", "database", "user", "password"];
    var nrOfFields = fieldNames.length;
    var passKey = fieldNames[nrOfFields - 1];
    function warn() {
      var isWritable = warnStream instanceof Stream && true === warnStream.writable;
      if (isWritable) {
        var args = Array.prototype.slice.call(arguments).concat("\n");
        warnStream.write(util.format.apply(util, args));
      }
    }
    __name(warn, "warn");
    __name2(warn, "warn");
    Object.defineProperty(module.exports, "isWin", {
      get: /* @__PURE__ */ __name2(function() {
        return isWin;
      }, "get"),
      set: /* @__PURE__ */ __name2(function(val) {
        isWin = val;
      }, "set")
    });
    module.exports.warnTo = function(stream) {
      var old = warnStream;
      warnStream = stream;
      return old;
    };
    module.exports.getFileName = function(rawEnv) {
      var env = rawEnv || process.env;
      var file = env.PGPASSFILE || (isWin ? path.join(env.APPDATA || "./", "postgresql", "pgpass.conf") : path.join(env.HOME || "./", ".pgpass"));
      return file;
    };
    module.exports.usePgPass = function(stats, fname) {
      if (Object.prototype.hasOwnProperty.call(process.env, "PGPASSWORD")) {
        return false;
      }
      if (isWin) {
        return true;
      }
      fname = fname || "<unkn>";
      if (!isRegFile(stats.mode)) {
        warn('WARNING: password file "%s" is not a plain file', fname);
        return false;
      }
      if (stats.mode & (S_IRWXG | S_IRWXO)) {
        warn('WARNING: password file "%s" has group or world access; permissions should be u=rw (0600) or less', fname);
        return false;
      }
      return true;
    };
    var matcher = module.exports.match = function(connInfo, entry) {
      return fieldNames.slice(0, -1).reduce(function(prev, field, idx) {
        if (idx == 1) {
          if (Number(connInfo[field] || defaultPort) === Number(entry[field])) {
            return prev && true;
          }
        }
        return prev && (entry[field] === "*" || entry[field] === connInfo[field]);
      }, true);
    };
    module.exports.getPassword = function(connInfo, stream, cb) {
      var pass;
      var lineStream = stream.pipe(split());
      function onLine(line) {
        var entry = parseLine(line);
        if (entry && isValidEntry(entry) && matcher(connInfo, entry)) {
          pass = entry[passKey];
          lineStream.end();
        }
      }
      __name(onLine, "onLine");
      __name2(onLine, "onLine");
      var onEnd = /* @__PURE__ */ __name2(function() {
        stream.destroy();
        cb(pass);
      }, "onEnd");
      var onErr = /* @__PURE__ */ __name2(function(err) {
        stream.destroy();
        warn("WARNING: error on reading file: %s", err);
        cb(void 0);
      }, "onErr");
      stream.on("error", onErr);
      lineStream.on("data", onLine).on("end", onEnd).on("error", onErr);
    };
    var parseLine = module.exports.parseLine = function(line) {
      if (line.length < 11 || line.match(/^\s+#/)) {
        return null;
      }
      var curChar = "";
      var prevChar = "";
      var fieldIdx = 0;
      var startIdx = 0;
      var endIdx = 0;
      var obj = {};
      var isLastField = false;
      var addToObj = /* @__PURE__ */ __name2(function(idx, i0, i1) {
        var field = line.substring(i0, i1);
        if (!Object.hasOwnProperty.call(process.env, "PGPASS_NO_DEESCAPE")) {
          field = field.replace(/\\([:\\])/g, "$1");
        }
        obj[fieldNames[idx]] = field;
      }, "addToObj");
      for (var i = 0; i < line.length - 1; i += 1) {
        curChar = line.charAt(i + 1);
        prevChar = line.charAt(i);
        isLastField = fieldIdx == nrOfFields - 1;
        if (isLastField) {
          addToObj(fieldIdx, startIdx);
          break;
        }
        if (i >= 0 && curChar == ":" && prevChar !== "\\") {
          addToObj(fieldIdx, startIdx, i + 1);
          startIdx = i + 2;
          fieldIdx += 1;
        }
      }
      obj = Object.keys(obj).length === nrOfFields ? obj : null;
      return obj;
    };
    var isValidEntry = module.exports.isValidEntry = function(entry) {
      var rules = {
        // host
        0: function(x) {
          return x.length > 0;
        },
        // port
        1: function(x) {
          if (x === "*") {
            return true;
          }
          x = Number(x);
          return isFinite(x) && x > 0 && x < 9007199254740992 && Math.floor(x) === x;
        },
        // database
        2: function(x) {
          return x.length > 0;
        },
        // username
        3: function(x) {
          return x.length > 0;
        },
        // password
        4: function(x) {
          return x.length > 0;
        }
      };
      for (var idx = 0; idx < fieldNames.length; idx += 1) {
        var rule = rules[idx];
        var value = entry[fieldNames[idx]] || "";
        var res = rule(value);
        if (!res) {
          return false;
        }
      }
      return true;
    };
  }
});
var require_lib = __commonJS({
  "node_modules/pgpass/lib/index.js"(exports, module) {
    "use strict";
    var path = require_path();
    var fs = require_fs();
    var helper = require_helper();
    module.exports = function(connInfo, cb) {
      var file = helper.getFileName();
      fs.stat(file, function(err, stat) {
        if (err || !helper.usePgPass(stat, file)) {
          return cb(void 0);
        }
        var st = fs.createReadStream(file);
        helper.getPassword(connInfo, st, cb);
      });
    };
    module.exports.warnTo = helper.warnTo;
  }
});
var require_client = __commonJS({
  "node_modules/pg/lib/client.js"(exports, module) {
    var EventEmitter = require_events().EventEmitter;
    var utils = require_utils();
    var nodeUtils = require_util();
    var sasl = require_sasl();
    var TypeOverrides2 = require_type_overrides();
    var ConnectionParameters = require_connection_parameters();
    var Query2 = require_query();
    var defaults2 = require_defaults();
    var Connection2 = require_connection();
    var crypto2 = require_utils2();
    var activeQueryDeprecationNotice = nodeUtils.deprecate(
      () => {
      },
      "Client.activeQuery is deprecated and will be removed in pg@9.0"
    );
    var queryQueueDeprecationNotice = nodeUtils.deprecate(
      () => {
      },
      "Client.queryQueue is deprecated and will be removed in pg@9.0."
    );
    var pgPassDeprecationNotice = nodeUtils.deprecate(
      () => {
      },
      "pgpass support is deprecated and will be removed in pg@9.0. You can provide an async function as the password property to the Client/Pool constructor that returns a password instead. Within this function you can call the pgpass module in your own code."
    );
    var byoPromiseDeprecationNotice = nodeUtils.deprecate(
      () => {
      },
      "Passing a custom Promise implementation to the Client/Pool constructor is deprecated and will be removed in pg@9.0."
    );
    var queryQueueLengthDeprecationNotice = nodeUtils.deprecate(
      () => {
      },
      "Calling client.query() when the client is already executing a query is deprecated and will be removed in pg@9.0. Use async/await or an external async flow control mechanism instead."
    );
    function coerceNumberOrDefault(value, defaultValue) {
      if (typeof value === "number") {
        return Number.isFinite(value) ? value : defaultValue;
      }
      if (typeof value === "string" && value.trim() !== "") {
        const n = Number(value);
        return Number.isFinite(n) ? n : defaultValue;
      }
      return defaultValue;
    }
    __name(coerceNumberOrDefault, "coerceNumberOrDefault");
    __name2(coerceNumberOrDefault, "coerceNumberOrDefault");
    var Client2 = class extends EventEmitter {
      static {
        __name(this, "Client2");
      }
      static {
        __name2(this, "Client");
      }
      constructor(config) {
        super();
        this.connectionParameters = new ConnectionParameters(config);
        this.user = this.connectionParameters.user;
        this.database = this.connectionParameters.database;
        this.port = this.connectionParameters.port;
        this.host = this.connectionParameters.host;
        Object.defineProperty(this, "password", {
          configurable: true,
          enumerable: false,
          writable: true,
          value: this.connectionParameters.password
        });
        this.replication = this.connectionParameters.replication;
        const c = config || {};
        if (c.Promise) {
          byoPromiseDeprecationNotice();
        }
        this._Promise = c.Promise || global.Promise;
        this._types = new TypeOverrides2(c.types);
        this._ending = false;
        this._ended = false;
        this._connecting = false;
        this._connected = false;
        this._connectionError = false;
        this._queryable = true;
        this._activeQuery = null;
        this._txStatus = null;
        this.enableChannelBinding = Boolean(c.enableChannelBinding);
        this.scramMaxIterations = coerceNumberOrDefault(c.scramMaxIterations, sasl.DEFAULT_MAX_SCRAM_ITERATIONS);
        this.connection = c.connection || new Connection2({
          stream: c.stream,
          ssl: this.connectionParameters.ssl,
          sslNegotiation: this.connectionParameters.sslnegotiation,
          keepAlive: c.keepAlive || false,
          keepAliveInitialDelayMillis: c.keepAliveInitialDelayMillis || 0,
          encoding: this.connectionParameters.client_encoding || "utf8"
        });
        this._queryQueue = [];
        this.binary = c.binary || defaults2.binary;
        this.processID = null;
        this.secretKey = null;
        this.ssl = this.connectionParameters.ssl || false;
        this.sslNegotiation = this.connectionParameters.sslnegotiation || "postgres";
        if (this.ssl && this.ssl.key) {
          Object.defineProperty(this.ssl, "key", {
            enumerable: false
          });
        }
        this._connectionTimeoutMillis = c.connectionTimeoutMillis || 0;
      }
      get activeQuery() {
        activeQueryDeprecationNotice();
        return this._activeQuery;
      }
      set activeQuery(val) {
        activeQueryDeprecationNotice();
        this._activeQuery = val;
      }
      _getActiveQuery() {
        return this._activeQuery;
      }
      _errorAllQueries(err) {
        const enqueueError = /* @__PURE__ */ __name2((query) => {
          process.nextTick(() => {
            query.handleError(err, this.connection);
          });
        }, "enqueueError");
        const activeQuery = this._getActiveQuery();
        if (activeQuery) {
          enqueueError(activeQuery);
          this._activeQuery = null;
        }
        this._queryQueue.forEach(enqueueError);
        this._queryQueue.length = 0;
      }
      _connect(callback) {
        const self = this;
        const con = this.connection;
        this._connectionCallback = callback;
        if (this._connecting || this._connected) {
          const err = new Error("Client has already been connected. You cannot reuse a client.");
          process.nextTick(() => {
            callback(err);
          });
          return;
        }
        this._connecting = true;
        if (this._connectionTimeoutMillis > 0) {
          this.connectionTimeoutHandle = setTimeout(() => {
            con._ending = true;
            con.stream.destroy(new Error("timeout expired"));
          }, this._connectionTimeoutMillis);
          if (this.connectionTimeoutHandle.unref) {
            this.connectionTimeoutHandle.unref();
          }
        }
        if (this.host && this.host.indexOf("/") === 0) {
          con.connect(this.host + "/.s.PGSQL." + this.port);
        } else {
          con.connect(this.port, this.host);
        }
        con.on("connect", function() {
          if (self.ssl) {
            if (self.sslNegotiation !== "direct") {
              con.requestSsl();
            }
          } else {
            con.startup(self.getStartupConf());
          }
        });
        con.on("sslconnect", function() {
          con.startup(self.getStartupConf());
        });
        this._attachListeners(con);
        con.once("end", () => {
          const error = this._ending ? new Error("Connection terminated") : new Error("Connection terminated unexpectedly");
          clearTimeout(this.connectionTimeoutHandle);
          this._errorAllQueries(error);
          this._ended = true;
          if (!this._ending) {
            if (this._connecting && !this._connectionError) {
              if (this._connectionCallback) {
                this._connectionCallback(error);
              } else {
                this._handleErrorEvent(error);
              }
            } else if (!this._connectionError) {
              this._handleErrorEvent(error);
            }
          }
          process.nextTick(() => {
            this.emit("end");
          });
        });
      }
      connect(callback) {
        if (callback) {
          this._connect(callback);
          return;
        }
        return new this._Promise((resolve, reject) => {
          this._connect((error) => {
            if (error) {
              reject(error);
            } else {
              resolve(this);
            }
          });
        });
      }
      _attachListeners(con) {
        con.on("authenticationCleartextPassword", this._handleAuthCleartextPassword.bind(this));
        con.on("authenticationMD5Password", this._handleAuthMD5Password.bind(this));
        con.on("authenticationSASL", this._handleAuthSASL.bind(this));
        con.on("authenticationSASLContinue", this._handleAuthSASLContinue.bind(this));
        con.on("authenticationSASLFinal", this._handleAuthSASLFinal.bind(this));
        con.on("backendKeyData", this._handleBackendKeyData.bind(this));
        con.on("error", this._handleErrorEvent.bind(this));
        con.on("errorMessage", this._handleErrorMessage.bind(this));
        con.on("readyForQuery", this._handleReadyForQuery.bind(this));
        con.on("notice", this._handleNotice.bind(this));
        con.on("rowDescription", this._handleRowDescription.bind(this));
        con.on("dataRow", this._handleDataRow.bind(this));
        con.on("portalSuspended", this._handlePortalSuspended.bind(this));
        con.on("emptyQuery", this._handleEmptyQuery.bind(this));
        con.on("commandComplete", this._handleCommandComplete.bind(this));
        con.on("parseComplete", this._handleParseComplete.bind(this));
        con.on("copyInResponse", this._handleCopyInResponse.bind(this));
        con.on("copyData", this._handleCopyData.bind(this));
        con.on("notification", this._handleNotification.bind(this));
      }
      _getPassword(cb) {
        const con = this.connection;
        if (typeof this.password === "function") {
          this._Promise.resolve().then(() => this.password(this.connectionParameters)).then((pass) => {
            if (pass !== void 0) {
              if (typeof pass !== "string") {
                con.emit("error", new TypeError("Password must be a string"));
                return;
              }
              this.connectionParameters.password = this.password = pass;
            } else {
              this.connectionParameters.password = this.password = null;
            }
            cb();
          }).catch((err) => {
            con.emit("error", err);
          });
        } else if (this.password !== null) {
          cb();
        } else {
          try {
            const pgPass = require_lib();
            pgPass(this.connectionParameters, (pass) => {
              if (void 0 !== pass) {
                pgPassDeprecationNotice();
                this.connectionParameters.password = this.password = pass;
              }
              cb();
            });
          } catch (e) {
            this.emit("error", e);
          }
        }
      }
      _handleAuthCleartextPassword(msg) {
        this._getPassword(() => {
          this.connection.password(this.password);
        });
      }
      _handleAuthMD5Password(msg) {
        this._getPassword(async () => {
          try {
            const hashedPassword = await crypto2.postgresMd5PasswordHash(this.user, this.password, msg.salt);
            this.connection.password(hashedPassword);
          } catch (e) {
            this.emit("error", e);
          }
        });
      }
      _handleAuthSASL(msg) {
        this._getPassword(() => {
          try {
            this.saslSession = sasl.startSession(
              msg.mechanisms,
              this.enableChannelBinding && this.connection.stream,
              this.scramMaxIterations
            );
            this.connection.sendSASLInitialResponseMessage(this.saslSession.mechanism, this.saslSession.response);
          } catch (err) {
            this.connection.emit("error", err);
          }
        });
      }
      async _handleAuthSASLContinue(msg) {
        try {
          await sasl.continueSession(
            this.saslSession,
            this.password,
            msg.data,
            this.enableChannelBinding && this.connection.stream
          );
          this.connection.sendSCRAMClientFinalMessage(this.saslSession.response);
        } catch (err) {
          this.connection.emit("error", err);
        }
      }
      _handleAuthSASLFinal(msg) {
        try {
          sasl.finalizeSession(this.saslSession, msg.data);
          this.saslSession = null;
        } catch (err) {
          this.connection.emit("error", err);
        }
      }
      _handleBackendKeyData(msg) {
        this.processID = msg.processID;
        this.secretKey = msg.secretKey;
      }
      _handleReadyForQuery(msg) {
        if (this._connecting) {
          this._connecting = false;
          this._connected = true;
          clearTimeout(this.connectionTimeoutHandle);
          if (this._connectionCallback) {
            this._connectionCallback(null, this);
            this._connectionCallback = null;
          }
          this.emit("connect");
        }
        const activeQuery = this._getActiveQuery();
        this._activeQuery = null;
        this._txStatus = msg?.status ?? null;
        this.readyForQuery = true;
        if (activeQuery) {
          activeQuery.handleReadyForQuery(this.connection);
        }
        this._pulseQueryQueue();
      }
      // if we receive an error event or error message
      // during the connection process we handle it here
      _handleErrorWhileConnecting(err) {
        if (this._connectionError) {
          return;
        }
        this._connectionError = true;
        clearTimeout(this.connectionTimeoutHandle);
        if (this._connectionCallback) {
          return this._connectionCallback(err);
        }
        this.emit("error", err);
      }
      // if we're connected and we receive an error event from the connection
      // this means the socket is dead - do a hard abort of all queries and emit
      // the socket error on the client as well
      _handleErrorEvent(err) {
        if (this._connecting) {
          return this._handleErrorWhileConnecting(err);
        }
        this._queryable = false;
        this._errorAllQueries(err);
        this.emit("error", err);
      }
      // handle error messages from the postgres backend
      _handleErrorMessage(msg) {
        if (this._connecting) {
          return this._handleErrorWhileConnecting(msg);
        }
        const activeQuery = this._getActiveQuery();
        if (!activeQuery) {
          this._handleErrorEvent(msg);
          return;
        }
        this._activeQuery = null;
        activeQuery.handleError(msg, this.connection);
      }
      _handleRowDescription(msg) {
        const activeQuery = this._getActiveQuery();
        if (activeQuery == null) {
          const error = new Error("Received unexpected rowDescription message from backend.");
          this._handleErrorEvent(error);
          return;
        }
        activeQuery.handleRowDescription(msg);
      }
      _handleDataRow(msg) {
        const activeQuery = this._getActiveQuery();
        if (activeQuery == null) {
          const error = new Error("Received unexpected dataRow message from backend.");
          this._handleErrorEvent(error);
          return;
        }
        activeQuery.handleDataRow(msg);
      }
      _handlePortalSuspended(msg) {
        const activeQuery = this._getActiveQuery();
        if (activeQuery == null) {
          const error = new Error("Received unexpected portalSuspended message from backend.");
          this._handleErrorEvent(error);
          return;
        }
        activeQuery.handlePortalSuspended(this.connection);
      }
      _handleEmptyQuery(msg) {
        const activeQuery = this._getActiveQuery();
        if (activeQuery == null) {
          const error = new Error("Received unexpected emptyQuery message from backend.");
          this._handleErrorEvent(error);
          return;
        }
        activeQuery.handleEmptyQuery(this.connection);
      }
      _handleCommandComplete(msg) {
        const activeQuery = this._getActiveQuery();
        if (activeQuery == null) {
          const error = new Error("Received unexpected commandComplete message from backend.");
          this._handleErrorEvent(error);
          return;
        }
        activeQuery.handleCommandComplete(msg, this.connection);
      }
      _handleParseComplete() {
        const activeQuery = this._getActiveQuery();
        if (activeQuery == null) {
          const error = new Error("Received unexpected parseComplete message from backend.");
          this._handleErrorEvent(error);
          return;
        }
        if (activeQuery.name) {
          this.connection.parsedStatements[activeQuery.name] = activeQuery.text;
        }
      }
      _handleCopyInResponse(msg) {
        const activeQuery = this._getActiveQuery();
        if (activeQuery == null) {
          const error = new Error("Received unexpected copyInResponse message from backend.");
          this._handleErrorEvent(error);
          return;
        }
        activeQuery.handleCopyInResponse(this.connection);
      }
      _handleCopyData(msg) {
        const activeQuery = this._getActiveQuery();
        if (activeQuery == null) {
          const error = new Error("Received unexpected copyData message from backend.");
          this._handleErrorEvent(error);
          return;
        }
        activeQuery.handleCopyData(msg, this.connection);
      }
      _handleNotification(msg) {
        this.emit("notification", msg);
      }
      _handleNotice(msg) {
        this.emit("notice", msg);
      }
      getStartupConf() {
        const params = this.connectionParameters;
        const data = {
          user: params.user,
          database: params.database
        };
        const appName = params.application_name || params.fallback_application_name;
        if (appName) {
          data.application_name = appName;
        }
        if (params.replication) {
          data.replication = "" + params.replication;
        }
        if (params.statement_timeout) {
          data.statement_timeout = String(parseInt(params.statement_timeout, 10));
        }
        if (params.lock_timeout) {
          data.lock_timeout = String(parseInt(params.lock_timeout, 10));
        }
        if (params.idle_in_transaction_session_timeout) {
          data.idle_in_transaction_session_timeout = String(parseInt(params.idle_in_transaction_session_timeout, 10));
        }
        if (params.options) {
          data.options = params.options;
        }
        return data;
      }
      cancel(client, query) {
        if (client.activeQuery === query) {
          const con = this.connection;
          if (this.host && this.host.indexOf("/") === 0) {
            con.connect(this.host + "/.s.PGSQL." + this.port);
          } else {
            con.connect(this.port, this.host);
          }
          con.on("connect", function() {
            con.cancel(client.processID, client.secretKey);
          });
        } else if (client._queryQueue.indexOf(query) !== -1) {
          client._queryQueue.splice(client._queryQueue.indexOf(query), 1);
        }
      }
      setTypeParser(oid, format, parseFn) {
        return this._types.setTypeParser(oid, format, parseFn);
      }
      getTypeParser(oid, format) {
        return this._types.getTypeParser(oid, format);
      }
      // escapeIdentifier and escapeLiteral moved to utility functions & exported
      // on PG
      // re-exported here for backwards compatibility
      escapeIdentifier(str) {
        return utils.escapeIdentifier(str);
      }
      escapeLiteral(str) {
        return utils.escapeLiteral(str);
      }
      _pulseQueryQueue() {
        if (this.readyForQuery === true) {
          this._activeQuery = this._queryQueue.shift();
          const activeQuery = this._getActiveQuery();
          if (activeQuery) {
            this.readyForQuery = false;
            this.hasExecuted = true;
            const queryError = activeQuery.submit(this.connection);
            if (queryError) {
              process.nextTick(() => {
                activeQuery.handleError(queryError, this.connection);
                this.readyForQuery = true;
                this._pulseQueryQueue();
              });
            }
          } else if (this.hasExecuted) {
            this._activeQuery = null;
            this.emit("drain");
          }
        }
      }
      query(config, values, callback) {
        let query;
        let result;
        if (config == null) {
          throw new TypeError("Client was passed a null or undefined query");
        }
        if (typeof config.submit === "function") {
          result = query = config;
          if (!query.callback) {
            if (typeof values === "function") {
              query.callback = values;
            } else if (callback) {
              query.callback = callback;
            }
          }
        } else {
          query = new Query2(config, values, callback);
          if (!query.callback) {
            result = new this._Promise((resolve, reject) => {
              query.callback = (err, res) => err ? reject(err) : resolve(res);
            }).catch((err) => {
              Error.captureStackTrace(err);
              throw err;
            });
          } else if (typeof query.callback !== "function") {
            throw new TypeError("callback is not a function");
          }
        }
        const readTimeout = config.query_timeout || this.connectionParameters.query_timeout;
        if (readTimeout) {
          const queryCallback = query.callback || (() => {
          });
          const readTimeoutTimer = setTimeout(() => {
            const error = new Error("Query read timeout");
            process.nextTick(() => {
              query.handleError(error, this.connection);
            });
            queryCallback(error);
            query.callback = () => {
            };
            const index = this._queryQueue.indexOf(query);
            if (index > -1) {
              this._queryQueue.splice(index, 1);
            }
            this._pulseQueryQueue();
          }, readTimeout);
          query.callback = (err, res) => {
            clearTimeout(readTimeoutTimer);
            queryCallback(err, res);
          };
        }
        if (this.binary && !query.binary) {
          query.binary = true;
        }
        if (query._result && !query._result._types) {
          query._result._types = this._types;
        }
        if (!this._queryable) {
          process.nextTick(() => {
            query.handleError(new Error("Client has encountered a connection error and is not queryable"), this.connection);
          });
          return result;
        }
        if (this._ending) {
          process.nextTick(() => {
            query.handleError(new Error("Client was closed and is not queryable"), this.connection);
          });
          return result;
        }
        if (this._queryQueue.length > 0) {
          queryQueueLengthDeprecationNotice();
        }
        this._queryQueue.push(query);
        this._pulseQueryQueue();
        return result;
      }
      ref() {
        this.connection.ref();
      }
      unref() {
        this.connection.unref();
      }
      getTransactionStatus() {
        return this._txStatus;
      }
      end(cb) {
        this._ending = true;
        if (!this.connection._connecting || this._ended) {
          if (cb) {
            cb();
            return;
          } else {
            return this._Promise.resolve();
          }
        }
        if (this._getActiveQuery() || !this._queryable) {
          this.connection.stream.destroy();
        } else {
          this.connection.end();
        }
        if (cb) {
          this.connection.once("end", cb);
        } else {
          return new this._Promise((resolve) => {
            this.connection.once("end", resolve);
          });
        }
      }
      get queryQueue() {
        queryQueueDeprecationNotice();
        return this._queryQueue;
      }
    };
    Client2.Query = Query2;
    module.exports = Client2;
  }
});
var require_pg_pool = __commonJS({
  "node_modules/pg-pool/index.js"(exports, module) {
    "use strict";
    var EventEmitter = require_events().EventEmitter;
    var NOOP = /* @__PURE__ */ __name2(function() {
    }, "NOOP");
    var removeWhere = /* @__PURE__ */ __name2((list, predicate) => {
      const i = list.findIndex(predicate);
      return i === -1 ? void 0 : list.splice(i, 1)[0];
    }, "removeWhere");
    var IdleItem = class {
      static {
        __name(this, "IdleItem");
      }
      static {
        __name2(this, "IdleItem");
      }
      constructor(client, idleListener, timeoutId) {
        this.client = client;
        this.idleListener = idleListener;
        this.timeoutId = timeoutId;
      }
    };
    var PendingItem = class {
      static {
        __name(this, "PendingItem");
      }
      static {
        __name2(this, "PendingItem");
      }
      constructor(callback) {
        this.callback = callback;
      }
    };
    function throwOnDoubleRelease() {
      throw new Error("Release called on client which has already been released to the pool.");
    }
    __name(throwOnDoubleRelease, "throwOnDoubleRelease");
    __name2(throwOnDoubleRelease, "throwOnDoubleRelease");
    function promisify(Promise2, callback) {
      if (callback) {
        return { callback, result: void 0 };
      }
      let rej;
      let res;
      const cb = /* @__PURE__ */ __name2(function(err, client) {
        err ? rej(err) : res(client);
      }, "cb");
      const result = new Promise2(function(resolve, reject) {
        res = resolve;
        rej = reject;
      }).catch((err) => {
        Error.captureStackTrace(err);
        throw err;
      });
      return { callback: cb, result };
    }
    __name(promisify, "promisify");
    __name2(promisify, "promisify");
    function makeIdleListener(pool, client) {
      return /* @__PURE__ */ __name2(/* @__PURE__ */ __name(function idleListener(err) {
        err.client = client;
        client.removeListener("error", idleListener);
        client.on("error", () => {
          pool.log("additional client error after disconnection due to error", err);
        });
        pool._remove(client);
        pool.emit("error", err, client);
      }, "idleListener"), "idleListener");
    }
    __name(makeIdleListener, "makeIdleListener");
    __name2(makeIdleListener, "makeIdleListener");
    var Pool2 = class extends EventEmitter {
      static {
        __name(this, "Pool2");
      }
      static {
        __name2(this, "Pool");
      }
      constructor(options, Client2) {
        super();
        this.options = Object.assign({}, options);
        if (options != null && "password" in options) {
          Object.defineProperty(this.options, "password", {
            configurable: true,
            enumerable: false,
            writable: true,
            value: options.password
          });
        }
        if (options != null && options.ssl && options.ssl.key) {
          Object.defineProperty(this.options.ssl, "key", {
            enumerable: false
          });
        }
        this.options.max = this.options.max || this.options.poolSize || 10;
        this.options.min = this.options.min || 0;
        this.options.maxUses = this.options.maxUses || Infinity;
        this.options.allowExitOnIdle = this.options.allowExitOnIdle || false;
        this.options.maxLifetimeSeconds = this.options.maxLifetimeSeconds || 0;
        this.log = this.options.log || function() {
        };
        this.Client = this.options.Client || Client2 || require_lib2().Client;
        this.Promise = this.options.Promise || global.Promise;
        if (typeof this.options.idleTimeoutMillis === "undefined") {
          this.options.idleTimeoutMillis = 1e4;
        }
        this._clients = [];
        this._idle = [];
        this._expired = /* @__PURE__ */ new WeakSet();
        this._pendingQueue = [];
        this._endCallback = void 0;
        this.ending = false;
        this.ended = false;
      }
      _promiseTry(f) {
        const Promise2 = this.Promise;
        if (typeof Promise2.try === "function") {
          return Promise2.try(f);
        }
        return new Promise2((resolve) => resolve(f()));
      }
      _isFull() {
        return this._clients.length >= this.options.max;
      }
      _isAboveMin() {
        return this._clients.length > this.options.min;
      }
      _pulseQueue() {
        this.log("pulse queue");
        if (this.ended) {
          this.log("pulse queue ended");
          return;
        }
        if (this.ending) {
          this.log("pulse queue on ending");
          if (this._idle.length) {
            this._idle.slice().map((item) => {
              this._remove(item.client);
            });
          }
          if (!this._clients.length) {
            this.ended = true;
            this._endCallback();
          }
          return;
        }
        if (!this._pendingQueue.length) {
          this.log("no queued requests");
          return;
        }
        if (!this._idle.length && this._isFull()) {
          return;
        }
        const pendingItem = this._pendingQueue.shift();
        if (this._idle.length) {
          const idleItem = this._idle.pop();
          clearTimeout(idleItem.timeoutId);
          const client = idleItem.client;
          client.ref && client.ref();
          const idleListener = idleItem.idleListener;
          return this._acquireClient(client, pendingItem, idleListener, false);
        }
        if (!this._isFull()) {
          return this.newClient(pendingItem);
        }
        throw new Error("unexpected condition");
      }
      _remove(client, callback) {
        const removed = removeWhere(this._idle, (item) => item.client === client);
        if (removed !== void 0) {
          clearTimeout(removed.timeoutId);
        }
        this._clients = this._clients.filter((c) => c !== client);
        const context = this;
        client.end(() => {
          context.emit("remove", client);
          if (typeof callback === "function") {
            callback();
          }
        });
      }
      connect(cb) {
        if (this.ending) {
          const err = new Error("Cannot use a pool after calling end on the pool");
          return cb ? cb(err) : this.Promise.reject(err);
        }
        const response = promisify(this.Promise, cb);
        const result = response.result;
        if (this._isFull() || this._idle.length) {
          if (this._idle.length) {
            process.nextTick(() => this._pulseQueue());
          }
          if (!this.options.connectionTimeoutMillis) {
            this._pendingQueue.push(new PendingItem(response.callback));
            return result;
          }
          const queueCallback = /* @__PURE__ */ __name2((err, res, done) => {
            clearTimeout(tid);
            response.callback(err, res, done);
          }, "queueCallback");
          const pendingItem = new PendingItem(queueCallback);
          const tid = setTimeout(() => {
            removeWhere(this._pendingQueue, (i) => i.callback === queueCallback);
            pendingItem.timedOut = true;
            response.callback(new Error("timeout exceeded when trying to connect"));
          }, this.options.connectionTimeoutMillis);
          if (tid.unref) {
            tid.unref();
          }
          this._pendingQueue.push(pendingItem);
          return result;
        }
        this.newClient(new PendingItem(response.callback));
        return result;
      }
      newClient(pendingItem) {
        const client = new this.Client(this.options);
        this._clients.push(client);
        const idleListener = makeIdleListener(this, client);
        this.log("checking client timeout");
        let tid;
        let timeoutHit = false;
        if (this.options.connectionTimeoutMillis) {
          tid = setTimeout(() => {
            if (client.connection) {
              this.log("ending client due to timeout");
              timeoutHit = true;
              client.connection.stream.destroy();
            } else if (!client.isConnected()) {
              this.log("ending client due to timeout");
              timeoutHit = true;
              client.end();
            }
          }, this.options.connectionTimeoutMillis);
        }
        this.log("connecting new client");
        client.connect((err) => {
          if (tid) {
            clearTimeout(tid);
          }
          client.on("error", idleListener);
          if (err) {
            this.log("client failed to connect", err);
            this._clients = this._clients.filter((c) => c !== client);
            if (timeoutHit) {
              err = new Error("Connection terminated due to connection timeout", { cause: err });
            }
            this._pulseQueue();
            if (!pendingItem.timedOut) {
              pendingItem.callback(err, void 0, NOOP);
            }
          } else {
            this.log("new client connected");
            if (this.options.onConnect) {
              this._promiseTry(() => this.options.onConnect(client)).then(
                () => {
                  this._afterConnect(client, pendingItem, idleListener);
                },
                (hookErr) => {
                  this._clients = this._clients.filter((c) => c !== client);
                  client.end(() => {
                    this._pulseQueue();
                    if (!pendingItem.timedOut) {
                      pendingItem.callback(hookErr, void 0, NOOP);
                    }
                  });
                }
              );
              return;
            }
            return this._afterConnect(client, pendingItem, idleListener);
          }
        });
      }
      _afterConnect(client, pendingItem, idleListener) {
        if (this.options.maxLifetimeSeconds !== 0) {
          const maxLifetimeTimeout = setTimeout(() => {
            this.log("ending client due to expired lifetime");
            this._expired.add(client);
            const idleIndex = this._idle.findIndex((idleItem) => idleItem.client === client);
            if (idleIndex !== -1) {
              this._acquireClient(
                client,
                new PendingItem((err, client2, clientRelease) => clientRelease()),
                idleListener,
                false
              );
            }
          }, this.options.maxLifetimeSeconds * 1e3);
          maxLifetimeTimeout.unref();
          client.once("end", () => clearTimeout(maxLifetimeTimeout));
        }
        return this._acquireClient(client, pendingItem, idleListener, true);
      }
      // acquire a client for a pending work item
      _acquireClient(client, pendingItem, idleListener, isNew) {
        if (isNew) {
          this.emit("connect", client);
        }
        this.emit("acquire", client);
        client.release = this._releaseOnce(client, idleListener);
        client.removeListener("error", idleListener);
        if (!pendingItem.timedOut) {
          if (isNew && this.options.verify) {
            this.options.verify(client, (err) => {
              if (err) {
                client.release(err);
                return pendingItem.callback(err, void 0, NOOP);
              }
              pendingItem.callback(void 0, client, client.release);
            });
          } else {
            pendingItem.callback(void 0, client, client.release);
          }
        } else {
          if (isNew && this.options.verify) {
            this.options.verify(client, client.release);
          } else {
            client.release();
          }
        }
      }
      // returns a function that wraps _release and throws if called more than once
      _releaseOnce(client, idleListener) {
        let released = false;
        return (err) => {
          if (released) {
            throwOnDoubleRelease();
          }
          released = true;
          this._release(client, idleListener, err);
        };
      }
      // release a client back to the poll, include an error
      // to remove it from the pool
      _release(client, idleListener, err) {
        client.on("error", idleListener);
        client._poolUseCount = (client._poolUseCount || 0) + 1;
        this.emit("release", err, client);
        if (err || this.ending || !client._queryable || client._ending || client._poolUseCount >= this.options.maxUses) {
          if (client._poolUseCount >= this.options.maxUses) {
            this.log("remove expended client");
          }
          return this._remove(client, this._pulseQueue.bind(this));
        }
        const isExpired = this._expired.has(client);
        if (isExpired) {
          this.log("remove expired client");
          this._expired.delete(client);
          return this._remove(client, this._pulseQueue.bind(this));
        }
        let tid;
        if (this.options.idleTimeoutMillis && this._isAboveMin()) {
          tid = setTimeout(() => {
            if (this._isAboveMin()) {
              this.log("remove idle client");
              this._remove(client, this._pulseQueue.bind(this));
            }
          }, this.options.idleTimeoutMillis);
          if (this.options.allowExitOnIdle) {
            tid.unref();
          }
        }
        if (this.options.allowExitOnIdle) {
          client.unref();
        }
        this._idle.push(new IdleItem(client, idleListener, tid));
        this._pulseQueue();
      }
      query(text, values, cb) {
        if (typeof text === "function") {
          const response2 = promisify(this.Promise, text);
          setImmediate(function() {
            return response2.callback(new Error("Passing a function as the first parameter to pool.query is not supported"));
          });
          return response2.result;
        }
        if (typeof values === "function") {
          cb = values;
          values = void 0;
        }
        const response = promisify(this.Promise, cb);
        cb = response.callback;
        this.connect((err, client) => {
          if (err) {
            return cb(err);
          }
          let clientReleased = false;
          const onError = /* @__PURE__ */ __name2((err2) => {
            if (clientReleased) {
              return;
            }
            clientReleased = true;
            client.release(err2);
            cb(err2);
          }, "onError");
          client.once("error", onError);
          this.log("dispatching query");
          try {
            client.query(text, values, (err2, res) => {
              this.log("query dispatched");
              client.removeListener("error", onError);
              if (clientReleased) {
                return;
              }
              clientReleased = true;
              client.release(err2);
              if (err2) {
                return cb(err2);
              }
              return cb(void 0, res);
            });
          } catch (err2) {
            client.release(err2);
            return cb(err2);
          }
        });
        return response.result;
      }
      end(cb) {
        this.log("ending");
        if (this.ending) {
          const err = new Error("Called end on pool more than once");
          return cb ? cb(err) : this.Promise.reject(err);
        }
        this.ending = true;
        const promised = promisify(this.Promise, cb);
        this._endCallback = promised.callback;
        this._pulseQueue();
        return promised.result;
      }
      get waitingCount() {
        return this._pendingQueue.length;
      }
      get idleCount() {
        return this._idle.length;
      }
      get expiredCount() {
        return this._clients.reduce((acc, client) => acc + (this._expired.has(client) ? 1 : 0), 0);
      }
      get totalCount() {
        return this._clients.length;
      }
    };
    module.exports = Pool2;
  }
});
var require_query2 = __commonJS({
  "node_modules/pg/lib/native/query.js"(exports, module) {
    "use strict";
    var EventEmitter = require_events().EventEmitter;
    var util = require_util();
    var utils = require_utils();
    var NativeQuery = module.exports = function(config, values, callback) {
      EventEmitter.call(this);
      config = utils.normalizeQueryConfig(config, values, callback);
      this.text = config.text;
      this.values = config.values;
      this.name = config.name;
      this.queryMode = config.queryMode;
      this.callback = config.callback;
      this.state = "new";
      this._arrayMode = config.rowMode === "array";
      this._emitRowEvents = false;
      this.on(
        "newListener",
        function(event) {
          if (event === "row") this._emitRowEvents = true;
        }.bind(this)
      );
    };
    util.inherits(NativeQuery, EventEmitter);
    var errorFieldMap = {
      sqlState: "code",
      statementPosition: "position",
      messagePrimary: "message",
      context: "where",
      schemaName: "schema",
      tableName: "table",
      columnName: "column",
      dataTypeName: "dataType",
      constraintName: "constraint",
      sourceFile: "file",
      sourceLine: "line",
      sourceFunction: "routine"
    };
    NativeQuery.prototype.handleError = function(err) {
      const fields = this.native.pq.resultErrorFields();
      if (fields) {
        for (const key in fields) {
          const normalizedFieldName = errorFieldMap[key] || key;
          err[normalizedFieldName] = fields[key];
        }
      }
      if (this.callback) {
        this.callback(err);
      } else {
        this.emit("error", err);
      }
      this.state = "error";
    };
    NativeQuery.prototype.then = function(onSuccess, onFailure) {
      return this._getPromise().then(onSuccess, onFailure);
    };
    NativeQuery.prototype.catch = function(callback) {
      return this._getPromise().catch(callback);
    };
    NativeQuery.prototype._getPromise = function() {
      if (this._promise) return this._promise;
      this._promise = new Promise(
        function(resolve, reject) {
          this._once("end", resolve);
          this._once("error", reject);
        }.bind(this)
      );
      return this._promise;
    };
    NativeQuery.prototype.submit = function(client) {
      this.state = "running";
      const self = this;
      this.native = client.native;
      client.native.arrayMode = this._arrayMode;
      let after = /* @__PURE__ */ __name2(function(err, rows, results) {
        client.native.arrayMode = false;
        setImmediate(function() {
          self.emit("_done");
        });
        if (err) {
          return self.handleError(err);
        }
        if (self._emitRowEvents) {
          if (results.length > 1) {
            rows.forEach((rowOfRows, i) => {
              rowOfRows.forEach((row) => {
                self.emit("row", row, results[i]);
              });
            });
          } else {
            rows.forEach(function(row) {
              self.emit("row", row, results);
            });
          }
        }
        self.state = "end";
        self.emit("end", results);
        if (self.callback) {
          self.callback(null, results);
        }
      }, "after");
      if (process.domain) {
        after = process.domain.bind(after);
      }
      if (this.name) {
        if (this.name.length > 63) {
          console.error("Warning! Postgres only supports 63 characters for query names.");
          console.error("You supplied %s (%s)", this.name, this.name.length);
          console.error("This can cause conflicts and silent errors executing queries");
        }
        const values = (this.values || []).map(utils.prepareValue);
        if (client.namedQueries[this.name]) {
          if (this.text && client.namedQueries[this.name] !== this.text) {
            const err = new Error(`Prepared statements must be unique - '${this.name}' was used for a different statement`);
            return after(err);
          }
          return client.native.execute(this.name, values, after);
        }
        return client.native.prepare(this.name, this.text, values.length, function(err) {
          if (err) return after(err);
          client.namedQueries[self.name] = self.text;
          return self.native.execute(self.name, values, after);
        });
      } else if (this.values) {
        if (!Array.isArray(this.values)) {
          const err = new Error("Query values must be an array");
          return after(err);
        }
        const vals = this.values.map(utils.prepareValue);
        client.native.query(this.text, vals, after);
      } else if (this.queryMode === "extended") {
        client.native.query(this.text, [], after);
      } else {
        client.native.query(this.text, after);
      }
    };
  }
});
var require_client2 = __commonJS({
  "node_modules/pg/lib/native/client.js"(exports, module) {
    var nodeUtils = require_util();
    var Native;
    try {
      Native = __require2("pg-native");
    } catch (e) {
      throw e;
    }
    var TypeOverrides2 = require_type_overrides();
    var EventEmitter = require_events().EventEmitter;
    var util = require_util();
    var ConnectionParameters = require_connection_parameters();
    var NativeQuery = require_query2();
    var queryQueueLengthDeprecationNotice = nodeUtils.deprecate(
      () => {
      },
      "Calling client.query() when the client is already executing a query is deprecated and will be removed in pg@9.0. Use async/await or an external async flow control mechanism instead."
    );
    var Client2 = module.exports = function(config) {
      EventEmitter.call(this);
      config = config || {};
      this._Promise = config.Promise || global.Promise;
      this._types = new TypeOverrides2(config.types);
      this.native = new Native({
        types: this._types
      });
      this._queryQueue = [];
      this._ending = false;
      this._connecting = false;
      this._connected = false;
      this._queryable = true;
      const cp = this.connectionParameters = new ConnectionParameters(config);
      if (config.nativeConnectionString) cp.nativeConnectionString = config.nativeConnectionString;
      this.user = cp.user;
      Object.defineProperty(this, "password", {
        configurable: true,
        enumerable: false,
        writable: true,
        value: cp.password
      });
      this.database = cp.database;
      this.host = cp.host;
      this.port = cp.port;
      this.namedQueries = {};
    };
    Client2.Query = NativeQuery;
    util.inherits(Client2, EventEmitter);
    Client2.prototype._errorAllQueries = function(err) {
      const enqueueError = /* @__PURE__ */ __name2((query) => {
        process.nextTick(() => {
          query.native = this.native;
          query.handleError(err);
        });
      }, "enqueueError");
      if (this._hasActiveQuery()) {
        enqueueError(this._activeQuery);
        this._activeQuery = null;
      }
      this._queryQueue.forEach(enqueueError);
      this._queryQueue.length = 0;
    };
    Client2.prototype._connect = function(cb) {
      const self = this;
      if (this._connecting) {
        process.nextTick(() => cb(new Error("Client has already been connected. You cannot reuse a client.")));
        return;
      }
      this._connecting = true;
      this.connectionParameters.getLibpqConnectionString(function(err, conString) {
        if (self.connectionParameters.nativeConnectionString) conString = self.connectionParameters.nativeConnectionString;
        if (err) return cb(err);
        self.native.connect(conString, function(err2) {
          if (err2) {
            self.native.end();
            return cb(err2);
          }
          self._connected = true;
          self.native.on("error", function(err3) {
            self._queryable = false;
            self._errorAllQueries(err3);
            self.emit("error", err3);
          });
          self.native.on("notification", function(msg) {
            self.emit("notification", {
              channel: msg.relname,
              payload: msg.extra
            });
          });
          self.emit("connect");
          self._pulseQueryQueue(true);
          cb(null, this);
        });
      });
    };
    Client2.prototype.connect = function(callback) {
      if (callback) {
        this._connect(callback);
        return;
      }
      return new this._Promise((resolve, reject) => {
        this._connect((error) => {
          if (error) {
            reject(error);
          } else {
            resolve(this);
          }
        });
      });
    };
    Client2.prototype.query = function(config, values, callback) {
      let query;
      let result;
      let readTimeout;
      let readTimeoutTimer;
      let queryCallback;
      if (config === null || config === void 0) {
        throw new TypeError("Client was passed a null or undefined query");
      } else if (typeof config.submit === "function") {
        readTimeout = config.query_timeout || this.connectionParameters.query_timeout;
        result = query = config;
        if (typeof values === "function") {
          config.callback = values;
        }
      } else {
        readTimeout = config.query_timeout || this.connectionParameters.query_timeout;
        query = new NativeQuery(config, values, callback);
        if (!query.callback) {
          let resolveOut, rejectOut;
          result = new this._Promise((resolve, reject) => {
            resolveOut = resolve;
            rejectOut = reject;
          }).catch((err) => {
            Error.captureStackTrace(err);
            throw err;
          });
          query.callback = (err, res) => err ? rejectOut(err) : resolveOut(res);
        }
      }
      if (readTimeout) {
        queryCallback = query.callback || (() => {
        });
        readTimeoutTimer = setTimeout(() => {
          const error = new Error("Query read timeout");
          process.nextTick(() => {
            query.handleError(error, this.connection);
          });
          queryCallback(error);
          query.callback = () => {
          };
          const index = this._queryQueue.indexOf(query);
          if (index > -1) {
            this._queryQueue.splice(index, 1);
          }
          this._pulseQueryQueue();
        }, readTimeout);
        query.callback = (err, res) => {
          clearTimeout(readTimeoutTimer);
          queryCallback(err, res);
        };
      }
      if (!this._queryable) {
        query.native = this.native;
        process.nextTick(() => {
          query.handleError(new Error("Client has encountered a connection error and is not queryable"));
        });
        return result;
      }
      if (this._ending) {
        query.native = this.native;
        process.nextTick(() => {
          query.handleError(new Error("Client was closed and is not queryable"));
        });
        return result;
      }
      if (this._queryQueue.length > 0) {
        queryQueueLengthDeprecationNotice();
      }
      this._queryQueue.push(query);
      this._pulseQueryQueue();
      return result;
    };
    Client2.prototype.end = function(cb) {
      const self = this;
      this._ending = true;
      if (this._connecting && !this._connected) {
        this.once("connect", () => {
          this.end(() => {
          });
        });
      }
      let result;
      if (!cb) {
        result = new this._Promise(function(resolve, reject) {
          cb = /* @__PURE__ */ __name2((err) => err ? reject(err) : resolve(), "cb");
        });
      }
      this.native.end(function() {
        self._connected = false;
        self._errorAllQueries(new Error("Connection terminated"));
        process.nextTick(() => {
          self.emit("end");
          if (cb) cb();
        });
      });
      return result;
    };
    Client2.prototype._hasActiveQuery = function() {
      return this._activeQuery && this._activeQuery.state !== "error" && this._activeQuery.state !== "end";
    };
    Client2.prototype._pulseQueryQueue = function(initialConnection) {
      if (!this._connected) {
        return;
      }
      if (this._hasActiveQuery()) {
        return;
      }
      const query = this._queryQueue.shift();
      if (!query) {
        if (!initialConnection) {
          this.emit("drain");
        }
        return;
      }
      this._activeQuery = query;
      query.submit(this);
      const self = this;
      query.once("_done", function() {
        self._pulseQueryQueue();
      });
    };
    Client2.prototype.cancel = function(query) {
      if (this._activeQuery === query) {
        this.native.cancel(function() {
        });
      } else if (this._queryQueue.indexOf(query) !== -1) {
        this._queryQueue.splice(this._queryQueue.indexOf(query), 1);
      }
    };
    Client2.prototype.ref = function() {
    };
    Client2.prototype.unref = function() {
    };
    Client2.prototype.setTypeParser = function(oid, format, parseFn) {
      return this._types.setTypeParser(oid, format, parseFn);
    };
    Client2.prototype.getTypeParser = function(oid, format) {
      return this._types.getTypeParser(oid, format);
    };
    Client2.prototype.isConnected = function() {
      return this._connected;
    };
    Client2.prototype.getTransactionStatus = function() {
      return this.native.getTransactionStatus();
    };
  }
});
var require_native = __commonJS({
  "node_modules/pg/lib/native/index.js"(exports, module) {
    "use strict";
    module.exports = require_client2();
  }
});
var require_lib2 = __commonJS({
  "node_modules/pg/lib/index.js"(exports, module) {
    "use strict";
    var Client2 = require_client();
    var defaults2 = require_defaults();
    var Connection2 = require_connection();
    var Result2 = require_result();
    var utils = require_utils();
    var Pool2 = require_pg_pool();
    var TypeOverrides2 = require_type_overrides();
    var { DatabaseError: DatabaseError2 } = require_dist();
    var { escapeIdentifier: escapeIdentifier2, escapeLiteral: escapeLiteral2 } = require_utils();
    var poolFactory = /* @__PURE__ */ __name2((Client3) => {
      return class BoundPool extends Pool2 {
        static {
          __name(this, "BoundPool");
        }
        static {
          __name2(this, "BoundPool");
        }
        constructor(options) {
          super(options, Client3);
        }
      };
    }, "poolFactory");
    var PG = /* @__PURE__ */ __name2(function(clientConstructor2) {
      this.defaults = defaults2;
      this.Client = clientConstructor2;
      this.Query = this.Client.Query;
      this.Pool = poolFactory(this.Client);
      this._pools = [];
      this.Connection = Connection2;
      this.types = require_pg_types();
      this.DatabaseError = DatabaseError2;
      this.TypeOverrides = TypeOverrides2;
      this.escapeIdentifier = escapeIdentifier2;
      this.escapeLiteral = escapeLiteral2;
      this.Result = Result2;
      this.utils = utils;
    }, "PG");
    var clientConstructor = Client2;
    var forceNative = false;
    try {
      forceNative = !!process.env.NODE_PG_FORCE_NATIVE;
    } catch {
    }
    if (forceNative) {
      clientConstructor = require_native();
    }
    module.exports = new PG(clientConstructor);
    Object.defineProperty(module.exports, "native", {
      configurable: true,
      enumerable: false,
      get() {
        let native = null;
        try {
          native = new PG(require_native());
        } catch (err) {
          if (err.code !== "MODULE_NOT_FOUND") {
            throw err;
          }
        }
        Object.defineProperty(module.exports, "native", {
          value: native
        });
        return native;
      }
    });
  }
});
var import_lib = __toESM(require_lib2(), 1);
var Client = import_lib.default.Client;
var Pool = import_lib.default.Pool;
var Connection = import_lib.default.Connection;
var types = import_lib.default.types;
var Query = import_lib.default.Query;
var DatabaseError = import_lib.default.DatabaseError;
var escapeIdentifier = import_lib.default.escapeIdentifier;
var escapeLiteral = import_lib.default.escapeLiteral;
var Result = import_lib.default.Result;
var TypeOverrides = import_lib.default.TypeOverrides;
var defaults = import_lib.default.defaults;
var CORS = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Methods": "GET, POST, PUT, DELETE, OPTIONS",
  "Access-Control-Allow-Headers": "Authorization, Content-Type, Accept, X-NS-Auth, X-NS-Email, Email, X-Requested-With, X-NS-Server"
};
var worker_default = {
  async fetch(request, env, ctx) {
    if (request.method === "OPTIONS") return new Response(null, { status: 204, headers: CORS });
    const url = new URL(request.url);
    SPEED_ENV = env;
    SPEED_CTX = ctx || null;
    try {
      if (url.pathname === "/student/home" && request.method === "GET") {
        return await handleStudentHome(request, env, url, ctx);
      }
      if (url.pathname === "/auth/register" && request.method === "POST") {
        return await handleRegister(request, env);
      }
      if (url.pathname === "/auth/login" && request.method === "POST") {
        return await handleLogin(request, env);
      }
      if (url.pathname === "/auth/logout" && request.method === "POST") {
        const user = await getUserFromToken(request, env);
        if (!user) return json({ error: "Nie zalogowano" }, 401);
        const token = getBearerToken(request);
        await env.DB.prepare("DELETE FROM sessions WHERE token = ?").bind(token).run();
        return json({ ok: true });
      }
      if (url.pathname === "/recordings" && request.method === "POST") {
        const user = await getUserFromToken(request, env);
        if (!user) return json({ error: "Nie zalogowano" }, 401);
        return await handleUploadRecording(request, env, user);
      }
      if (url.pathname === "/recordings" && request.method === "GET") {
        const user = await getUserFromToken(request, env);
        if (!user) return json({ error: "Nie zalogowano" }, 401);
        return await handleListRecordings(request, env, user);
      }
      if (url.pathname === "/harmonogram-overview" && request.method === "GET") {
        const user = await getUserFromToken(request, env);
        if (!user) return json({ error: "Nie zalogowano" }, 401);
        if (user.role === "student") return json({ error: "Brak uprawnie\u0144" }, 403);
        return await handleHarmonogramOverview(env, user);
      }
      if (url.pathname === "/students" && request.method === "GET") {
        const user = await getUserFromToken(request, env);
        if (!user) return json({ error: "Nie zalogowano" }, 401);
        if (user.role === "student") return json({ error: "Brak uprawnie\u0144" }, 403);
        return await handleListStudents(request, env, user);
      }
      const reviewMatch = url.pathname.match(/^\/recordings\/(\d+)\/review$/);
      if (reviewMatch && request.method === "PUT") {
        const user = await getUserFromToken(request, env);
        if (!user) return json({ error: "Nie zalogowano" }, 401);
        if (user.role === "student") return json({ error: "Brak uprawnie\u0144" }, 403);
        return await handleReviewRecording(request, env, user, parseInt(reviewMatch[1]));
      }
      const correctionMatch = url.pathname.match(/^\/recordings\/(\d+)\/correction$/);
      if (correctionMatch && request.method === "POST") {
        const user = await getUserFromToken(request, env);
        if (!user) return json({ error: "Nie zalogowano" }, 401);
        if (user.role === "student") return json({ error: "Brak uprawnie\u0144" }, 403);
        return await handleUploadCorrection(request, env, user, parseInt(correctionMatch[1]));
      }
      if (url.pathname === "/requirements" && request.method === "POST") {
        const user = await getUserFromToken(request, env);
        if (!user) return json({ error: "Nie zalogowano" }, 401);
        if (user.role === "student") return json({ error: "Brak uprawnie\u0144" }, 403);
        return await handleSetRequirement(request, env, user);
      }
      if (url.pathname === "/requirements" && request.method === "GET") {
        const user = await getUserFromToken(request, env);
        if (!user) return json({ error: "Nie zalogowano" }, 401);
        return await handleGetRequirements(request, env, user);
      }
      const audioMatch = url.pathname.match(/^\/recordings\/(\d+)\/audio$/);
      if (audioMatch && request.method === "GET") {
        const user = await getUserFromToken(request, env);
        if (!user) return json({ error: "Nie zalogowano" }, 401);
        return await handleGetAudio(env, user, parseInt(audioMatch[1]));
      }
      const deleteMatch = url.pathname.match(/^\/recordings\/(\d+)$/);
      if (deleteMatch && request.method === "DELETE") {
        const user = await getUserFromToken(request, env);
        if (!user) return json({ error: "Nie zalogowano" }, 401);
        if (user.role !== "admin") return json({ error: "Tylko admin mo\u017Ce usuwa\u0107 nagrania" }, 403);
        return await handleDeleteRecording(env, user, parseInt(deleteMatch[1]));
      }
      const catChangeMatch = url.pathname.match(/^\/recordings\/(\d+)\/category$/);
      if (catChangeMatch && request.method === "PUT") {
        const user = await getUserFromToken(request, env);
        if (!user) return json({ error: "Nie zalogowano" }, 401);
        return await handleChangeCategory(request, env, user, parseInt(catChangeMatch[1]));
      }
      if (url.pathname === "/admin/assign-group" && request.method === "POST") {
        return await handleAssignGroup(request, env);
      }
      if (url.pathname === "/admin/set-requirement" && request.method === "POST") {
        return await handleSetRequirementByEmail(request, env);
      }
      if (url.pathname === "/admin/trainer-permissions/list" && request.method === "GET") {
        return await handleTrainerPermissionsList(request, env, url);
      }
      if (url.pathname === "/admin/trainer-permissions/save" && request.method === "POST") {
        return await handleTrainerPermissionsSave(request, env);
      }
      if (url.pathname === "/admin/disk-status" && request.method === "GET") {
        return await handleDiskStatus(request, env, url);
      }
      if (url.pathname === "/trainer-permissions/mine" && request.method === "GET") {
        return await handleTrainerPermissionsMine(request, env, url);
      }
      if (url.pathname === "/templates" && request.method === "POST") {
        const user = await getUserFromToken(request, env);
        if (!user) return json({ error: "Nie zalogowano" }, 401);
        if (user.role === "student") return json({ error: "Brak uprawnie\u0144" }, 403);
        return await handleCreateTemplate(request, env, user);
      }
      if (url.pathname === "/schedule-cache/list" && request.method === "GET") {
        return await handleScheduleCacheList(request, env, url);
      }
      if (url.pathname === "/schedule-cache/save" && request.method === "POST") {
        return await handleScheduleCacheSave(request, env);
      }
      if (url.pathname === "/harmonogram-cache/list" && request.method === "GET") {
        return await handleHarmonogramCacheList(request, env, url);
      }
      if (url.pathname === "/harmonogram-cache/save" && request.method === "POST") {
        return await handleHarmonogramCacheSave(request, env);
      }
      if (url.pathname === "/voice-review/upload" && request.method === "POST") {
        return await handleVoiceReviewUpload(request, env, url);
      }
      if (url.pathname === "/voice-review/audio" && request.method === "GET") {
        return await handleVoiceReviewAudio(request, env, url);
      }
      if (url.pathname === "/voice-review/check" && request.method === "GET") {
        return await handleVoiceReviewCheck(request, env, url);
      }
      if (url.pathname === "/voice-review/delete" && request.method === "DELETE") {
        return await handleVoiceReviewDelete(request, env, url);
      }
      if (url.pathname === "/diary-reply/get" && request.method === "GET") {
        return await handleDiaryReplyGet(request, env, url);
      }
      if (url.pathname === "/diary-reply/save-text" && request.method === "POST") {
        return await handleDiaryReplySaveText(request, env);
      }
      if (url.pathname === "/diary-reply/save-audio" && request.method === "POST") {
        return await handleDiaryReplySaveAudio(request, env, url);
      }
      if (url.pathname === "/diary-reply/audio" && request.method === "GET") {
        return await handleDiaryReplyAudio(request, env, url);
      }
      if (url.pathname === "/diary-reply/audio" && request.method === "DELETE") {
        return await handleDiaryReplyDeleteAudio(request, env, url);
      }
      if (url.pathname === "/trainer/visit-dates" && request.method === "GET") {
        return await handleTrainerVisitDates(request, env, url);
      }
      if (url.pathname === "/documents" && request.method === "GET") {
        return await handleDocumentsList(request, env, url);
      }
      if (url.pathname.startsWith("/documents/") && request.method === "POST") {
        return await handleDocumentsAction(request, env, url.pathname.slice("/documents/".length));
      }
      if (url.pathname === "/google/oauth/start" && request.method === "GET") {
        return await handleGoogleOAuthStart(request, env, url);
      }
      if (url.pathname === "/google/oauth/callback" && request.method === "GET") {
        return await handleGoogleOAuthCallback(request, env, url);
      }
      if (url.pathname === "/google/oauth/status" && request.method === "GET") {
        return await handleGoogleOAuthStatus(request, env, url);
      }
      if (url.pathname === "/disc/results" && request.method === "POST") {
        return await handleDiscSave(request, env);
      }
      if (url.pathname === "/disc/results" && request.method === "GET") {
        return await handleDiscList(request, env, url);
      }
      if (url.pathname === "/disc/overview" && request.method === "GET") {
        return await handleDiscOverview(request, env, url);
      }
      if (url.pathname === "/disc/results/delete" && request.method === "POST") {
        return await handleDiscDelete(request, env);
      }
      if (url.pathname === "/special-tasks" && request.method === "GET") {
        return await handleSpecialTasksList(request, env, url);
      }
      if (url.pathname === "/special-tasks/mine" && request.method === "GET") {
        return await handleSpecialTasksMine(request, env, url);
      }
      if ((url.pathname === "/special-tasks/credit" || url.pathname === "/special-tasks/uncredit") && request.method === "POST") {
        return await handleSpecialTasksCredit(request, env, url.pathname.endsWith("/uncredit"));
      }
      if (url.pathname === "/review-log" && request.method === "POST") {
        return await handleReviewLog(request, env);
      }
      if (url.pathname === "/review-log/counts" && request.method === "GET") {
        return await handleReviewLogCounts(request, env, url);
      }
      if (url.pathname === "/call-notices" && request.method === "GET") {
        return await handleCallNoticesList(request, env, url);
      }
      if (url.pathname === "/call-notices/clear" && request.method === "POST") {
        return await handleCallNoticesClear(request, env);
      }
      if (url.pathname === "/call-notices/mine" && request.method === "GET") {
        return await handleCallNoticesMine(request, env, url);
      }
      if (url.pathname === "/call-notices/ack" && request.method === "POST") {
        return await handleCallNoticesAck(request, env);
      }
      if (url.pathname === "/coach-settings" && request.method === "GET") {
        return await handleCoachSettingsGet(request, env, url);
      }
      if (url.pathname === "/coach-settings/save" && request.method === "POST") {
        return await handleCoachSettingsSave(request, env);
      }
      if (url.pathname === "/transcripts" && request.method === "GET") {
        return await handleTranscriptsGet(request, env, url);
      }
      if (url.pathname === "/talk-stats" && request.method === "GET") {
        const who = await contactAuth(env, url.searchParams.get("ns_token"), url.searchParams.get("ns_email"), url.searchParams.get("ns_server"));
        if (!who) return json({ error: "Nieprawid\u0142owy lub wygas\u0142y token NS \u2014 zaloguj si\u0119 ponownie" }, 401);
        if (who.forbidden) return json({ error: "Tylko trener/admin" }, 403);
        const sid = String(url.searchParams.get("student_id") || "");
        if (!sid) return json({ error: "Brak student_id" }, 400);
        return json(await txTalkStats(env, sid));
      }
      if (url.pathname === "/talk-stats/mine" && request.method === "GET") {
        const me = await coachStudentFromRequest(request, env, url, null);
        if (!me) return json({ error: "Nieprawid\u0142owy lub wygas\u0142y token NS \u2014 zaloguj si\u0119 ponownie" }, 401);
        if (!me.id) return json({ ok: true, categories: [], totals: {}, weeks: [] });
        return json(await txTalkStats(env, me.id));
      }
      if (url.pathname === "/trainer-uploader" && request.method === "GET") {
        const who = await contactAuth(env, url.searchParams.get("ns_token"), url.searchParams.get("ns_email"), url.searchParams.get("ns_server"));
        if (!who) return json({ error: "Nieprawid\u0142owy lub wygas\u0142y token NS \u2014 zaloguj si\u0119 ponownie" }, 401);
        if (who.forbidden) return json({ error: "Tylko trener/admin" }, 403);
        await ensureTranscriptTables(env);
        const sid = String(url.searchParams.get("student_id") || "");
        const row = sid ? await env.DB.prepare("SELECT ns_user_id FROM trainer_uploader WHERE ns_user_id = ?").bind(sid).first() : null;
        return json({ ok: true, on: !!row });
      }
      if (url.pathname === "/trainer-uploader/set" && request.method === "POST") {
        let body = {};
        try {
          body = await request.json();
        } catch (e) {
        }
        const who = await contactAuth(env, body.ns_token, body.ns_email, body.ns_server);
        if (!who) return json({ error: "Nieprawid\u0142owy lub wygas\u0142y token NS \u2014 zaloguj si\u0119 ponownie" }, 401);
        if (who.forbidden) return json({ error: "Tylko trener/admin" }, 403);
        await ensureTranscriptTables(env);
        const sid = String(body.student_id || "");
        if (!sid) return json({ error: "Brak student_id" }, 400);
        if (body.on) await env.DB.prepare("INSERT INTO trainer_uploader (ns_user_id, set_by, created_at) VALUES (?, ?, ?) ON CONFLICT(ns_user_id) DO NOTHING").bind(sid, who.email || "", Math.floor(Date.now() / 1e3)).run();
        else await env.DB.prepare("DELETE FROM trainer_uploader WHERE ns_user_id = ?").bind(sid).run();
        await env.DB.prepare("UPDATE record_transcript SET speakers = NULL, spk_names = NULL, spk_src = NULL WHERE ns_student_id = ? AND spk_src LIKE 'ai-%'").bind(sid).run();
        TX_TRAINER_SET = null;
        return json({ ok: true, on: !!body.on });
      }
      if (url.pathname === "/transcripts/edit" && request.method === "POST") {
        return await handleTranscriptEdit(request, env);
      }
      if (url.pathname === "/transcripts/fix" && request.method === "POST") {
        return await handleTranscriptFix(request, env);
      }
      if (url.pathname === "/transcripts/delete" && request.method === "POST") {
        return await handleTranscriptDelete(request, env);
      }
      if (url.pathname === "/transcripts/summary" && request.method === "POST") {
        return await handleTranscriptSummary(request, env);
      }
      if (url.pathname === "/transcripts/speakers" && request.method === "POST") {
        return await handleTranscriptSpeakers(request, env);
      }
      if (url.pathname === "/transcripts/day-monolog" && request.method === "GET") {
        return await handleTranscriptsDayMonolog(request, env, url);
      }
      if (url.pathname === "/transcripts/status" && request.method === "GET") {
        return await handleTranscriptsStatus(request, env, url);
      }
      if (url.pathname === "/transcripts/corpus" && request.method === "GET") {
        return await handleTranscriptsCorpus(request, env, url);
      }
      if (url.pathname === "/transcripts/student" && request.method === "GET") {
        return await handleTranscriptsStudent(request, env, url);
      }
      if (url.pathname === "/transcripts/run" && request.method === "POST") {
        return await handleTranscriptRun(request, env);
      }
      if (url.pathname === "/record-fp" && request.method === "GET") {
        return await handleRecordFpList(request, env, url);
      }
      if (url.pathname === "/record-fp/save" && request.method === "POST") {
        return await handleRecordFpSave(request, env);
      }
      if (url.pathname === "/record-system" && request.method === "GET") {
        return await handleRecordSystemGet(request, env, url);
      }
      if (url.pathname === "/record-system/save" && request.method === "POST") {
        return await handleRecordSystemSave(request, env);
      }
      if (url.pathname === "/focus" && request.method === "GET") {
        return await handleFocusGet(request, env, url);
      }
      if (url.pathname === "/focus/save" && request.method === "POST") {
        return await handleFocusSave(request, env);
      }
      if (url.pathname === "/focus/mine" && request.method === "GET") {
        return await handleFocusMine(request, env, url);
      }
      if (url.pathname === "/email/send-plan" && request.method === "POST") {
        return await handleEmailSendPlan(request, env);
      }
      if (url.pathname === "/google/oauth/disconnect" && request.method === "POST") {
        return await handleGoogleOAuthDisconnect(request, env);
      }
      if (url.pathname === "/student-ack" && request.method === "POST") {
        return await handleStudentAckSave(request, env);
      }
      if (url.pathname === "/student-ack/mine" && request.method === "GET") {
        return await handleStudentAckMine(request, env, url);
      }
      if (url.pathname === "/student-acks" && request.method === "GET") {
        return await handleStudentAcksList(request, env, url);
      }
      if (url.pathname === "/student-acks/seen" && request.method === "POST") {
        return await handleStudentAcksSeen(request, env);
      }
      if (url.pathname === "/ns-record-audio" && request.method === "GET") {
        return await handleNsRecordAudio(request, env, url);
      }
      if (url.pathname === "/call-days/mine" && request.method === "GET") {
        return await handleMyCallDays(request, env, url);
      }
      if (url.pathname === "/call-days/calendar.ics" && request.method === "GET") {
        return await handleCallDaysIcs(request, env, url);
      }
      if (url.pathname === "/call-days" && request.method === "GET") {
        return await handleCallDaysList(request, env, url);
      }
      if (url.pathname.startsWith("/call-days/") && request.method === "POST") {
        return await handleCallDaysAction(request, env, url.pathname.slice("/call-days/".length));
      }
      if (url.pathname === "/contact-lists/mine" && request.method === "GET") {
        return await handleMyContactList(request, env, url);
      }
      if (url.pathname === "/contact-lists" && request.method === "GET") {
        return await handleContactLists(request, env, url);
      }
      if (url.pathname.startsWith("/contact-lists/") && request.method === "POST") {
        return await handleContactListsAction(request, env, url.pathname.slice("/contact-lists/".length));
      }
      if (url.pathname === "/favorites/toggle" && request.method === "POST") {
        return await handleFavoriteToggle(request, env);
      }
      if (url.pathname === "/favorites/note" && request.method === "POST") {
        return await handleFavoriteNote(request, env);
      }
      if (url.pathname === "/favorites/list" && request.method === "GET") {
        return await handleFavoritesList(request, env, url);
      }
      if (url.pathname === "/admin/set-student-setup" && request.method === "POST") {
        return await handleSetStudentSetup(request, env);
      }
      if (url.pathname === "/admin/force-delete-record" && request.method === "POST") {
        return await handleForceDeleteRecord(request, env);
      }
      if (url.pathname === "/admin/period-stats" && request.method === "GET") {
        return await handleAdminPeriodStats(request, env, url);
      }
      if (url.pathname === "/trainer/gps-points" && request.method === "GET") {
        return await handleTrainerGpsPoints(request, env, url);
      }
      if (url.pathname.startsWith("/ns-proxy/")) {
        return await handleNsProxy(request, url, env);
      }
      if (url.pathname === "/audio-proxy" && request.method === "GET") {
        return await handleAudioProxy(request, env);
      }
      if (url.pathname === "/public-templates" && request.method === "GET") {
        return await handlePublicTemplatesList(env);
      }
      if (url.pathname === "/public-templates" && request.method === "POST") {
        return await handlePublicTemplateCreate(request, env);
      }
      const publicTplDeleteMatch = url.pathname.match(/^\/public-templates\/(\d+)$/);
      if (publicTplDeleteMatch && request.method === "DELETE") {
        return await handlePublicTemplateDelete(request, env, parseInt(publicTplDeleteMatch[1]));
      }
      if (url.pathname === "/categories" && request.method === "GET") {
        const user = await getUserFromToken(request, env);
        if (!user) return json({ error: "Nie zalogowano" }, 401);
        const { results } = await env.DB.prepare("SELECT key, name_pl as name FROM categories ORDER BY name_pl").all();
        return json({ ok: true, categories: results });
      }
      if (url.pathname === "/templates" && request.method === "GET") {
        const user = await getUserFromToken(request, env);
        if (!user) return json({ error: "Nie zalogowano" }, 401);
        if (user.role === "student") return json({ error: "Brak uprawnie\u0144" }, 403);
        return await handleListTemplates(env, user);
      }
      const applyTemplateMatch = url.pathname.match(/^\/templates\/(\d+)\/apply$/);
      if (applyTemplateMatch && request.method === "POST") {
        const user = await getUserFromToken(request, env);
        if (!user) return json({ error: "Nie zalogowano" }, 401);
        if (user.role === "student") return json({ error: "Brak uprawnie\u0144" }, 403);
        return await handleApplyTemplate(request, env, user, parseInt(applyTemplateMatch[1]));
      }
      if (url.pathname === "/diary" && request.method === "POST") {
        const user = await getUserFromToken(request, env);
        if (!user) return json({ error: "Nie zalogowano" }, 401);
        return await handleSubmitDiary(request, env, user);
      }
      if (url.pathname === "/diary" && request.method === "GET") {
        const user = await getUserFromToken(request, env);
        if (!user) return json({ error: "Nie zalogowano" }, 401);
        return await handleGetDiary(request, env, user);
      }
      if (url.pathname === "/diary/from-ns" && request.method === "POST") {
        const user = await requireNsBridgeUser(request, env);
        if (!user) return json({ error: "Nieprawid\u0142owy lub wygas\u0142y token NS" }, 401);
        return await handleSubmitDiary(request, env, user);
      }
      if (url.pathname === "/diary/from-ns" && request.method === "GET") {
        const user = await requireNsBridgeUser(request, env);
        if (!user) return json({ error: "Nieprawid\u0142owy lub wygas\u0142y token NS" }, 401);
        let overrideUserId;
        const nsStudentId = url.searchParams.get("ns_student_id");
        if (nsStudentId) {
          const student = await env.DB.prepare("SELECT id FROM users WHERE ns_legacy_id = ?").bind(nsStudentId).first();
          if (!student) return json({ error: "Nie znaleziono tego kursanta w tym systemie (mo\u017Ce jeszcze nie zaimportowany)" }, 404);
          overrideUserId = student.id;
        }
        return await handleGetDiary(request, env, user, overrideUserId);
      }
      if (url.pathname === "/diary/unread-from-ns" && request.method === "GET") {
        const user = await requireNsBridgeUser(request, env);
        if (!user) return json({ error: "Nieprawid\u0142owy lub wygas\u0142y token NS" }, 401);
        if (user.role === "student") return json({ error: "To konto nie ma roli trenera w tym systemie \u2014 popro\u015B administratora o nadanie dost\u0119pu" }, 403);
        return await handleUnreadDiaryFromNs(env, user);
      }
      const nsMarkReadMatch = url.pathname.match(/^\/diary\/(\d+)\/mark-read-from-ns$/);
      if (nsMarkReadMatch && request.method === "PUT") {
        const user = await requireNsBridgeUser(request, env);
        if (!user) return json({ error: "Nieprawid\u0142owy lub wygas\u0142y token NS" }, 401);
        if (user.role === "student") return json({ error: "To konto nie ma roli trenera w tym systemie \u2014 popro\u015B administratora o nadanie dost\u0119pu" }, 403);
        const diaryId = parseInt(nsMarkReadMatch[1]);
        const row = await env.DB.prepare("SELECT user_id, entry_date FROM diary_entries WHERE id = ?").bind(diaryId).first();
        if (row) {
          await env.DB.prepare(
            "UPDATE diary_entries SET trainer_read_at = ? WHERE user_id = ? AND entry_date = ? AND trainer_read_at IS NULL"
          ).bind(Math.floor(Date.now() / 1e3), row.user_id, row.entry_date).run();
        } else {
          await env.DB.prepare("UPDATE diary_entries SET trainer_read_at = ? WHERE id = ?").bind(Math.floor(Date.now() / 1e3), diaryId).run();
        }
        return json({ ok: true });
      }
      if (url.pathname === "/needs-correction/from-ns" && request.method === "GET") {
        const user = await requireNsBridgeUser(request, env);
        if (!user) return json({ error: "Nieprawid\u0142owy lub wygas\u0142y token NS" }, 401);
        const syncState = await env.DB.prepare(
          "SELECT last_synced_at FROM needs_correction_sync_state WHERE user_id = ?"
        ).bind(user.id).first();
        const ageMs = syncState ? Date.now() - syncState.last_synced_at * 1e3 : Infinity;
        const forceRefresh = url.searchParams.get("force") === "1";
        if (!syncState) {
          await syncNeedsCorrectionFromPostgres(env, user.id, user.email);
        } else if (forceRefresh || ageMs > 12e4) {
          speedLater(syncNeedsCorrectionFromPostgres(env, user.id, user.email));
        }
        const { results } = await env.DB.prepare(
          "SELECT * FROM needs_correction_cache WHERE user_id = ? ORDER BY COALESCE(recorded_at, reviewed_at) ASC"
        ).bind(user.id).all();
        return json({
          ok: true,
          count: results.length,
          // recorded_at = kiedy przyszla AKTUALNA wersja nagrania (unix, s); first_sent_at = pierwsze wyslanie;
          // record_date = dzien w NS; review_details = komentarz trenera; not_counted = trener uznal nagranie
          // za te sama sytuacje / ponownie wyslane (komentarz zaczyna sie od 🚫) — NIE wliczone do harmonogramu
          entries: results.map(correctionEntryOut)
        });
      }
      if (url.pathname === "/harmonogram-stats/from-ns" && request.method === "GET") {
        const user = await requireNsBridgeUser(request, env);
        if (!user) return json({ error: "Nieprawid\u0142owy lub wygas\u0142y token NS" }, 401);
        const dateFrom = url.searchParams.get("date_from") || "1970-01-01";
        const dateTo = url.searchParams.get("date_to") || "2999-12-31";
        const stats = await harmoStatsCached(env, user.email, dateFrom, dateTo);
        return json({ ok: true, ...stats });
      }
      if (url.pathname === "/dashboard" && request.method === "GET") {
        const user = await getUserFromToken(request, env);
        if (!user) return json({ error: "Nie zalogowano" }, 401);
        if (user.role === "student") return json({ error: "Brak uprawnie\u0144" }, 403);
        return await handleDashboard(env, user);
      }
      const studentDetailMatch = url.pathname.match(/^\/students\/(\d+)\/detail$/);
      if (studentDetailMatch && request.method === "GET") {
        const user = await getUserFromToken(request, env);
        if (!user) return json({ error: "Nie zalogowano" }, 401);
        if (user.role === "student") return json({ error: "Brak uprawnie\u0144" }, 403);
        return await handleStudentDetail(env, user, parseInt(studentDetailMatch[1]));
      }
      const ratingsMatch = url.pathname.match(/^\/recordings\/(\d+)\/ratings$/);
      if (ratingsMatch && request.method === "PUT") {
        const user = await getUserFromToken(request, env);
        if (!user) return json({ error: "Nie zalogowano" }, 401);
        if (user.role === "student") return json({ error: "Brak uprawnie\u0144" }, 403);
        return await handleSaveRatings(request, env, user, parseInt(ratingsMatch[1]));
      }
      if (url.pathname === "/rating-criteria" && request.method === "GET") {
        const user = await getUserFromToken(request, env);
        if (!user) return json({ error: "Nie zalogowano" }, 401);
        return await handleListCriteria(env);
      }
      if (url.pathname === "/groups" && request.method === "GET") {
        const user = await getUserFromToken(request, env);
        if (!user) return json({ error: "Nie zalogowano" }, 401);
        if (user.role === "student") return json({ error: "Brak uprawnie\u0144" }, 403);
        return await handleListGroupsDetailed(env);
      }
      if (url.pathname === "/groups" && request.method === "POST") {
        const user = await getUserFromToken(request, env);
        if (!user) return json({ error: "Nie zalogowano" }, 401);
        if (user.role !== "admin") return json({ error: "Tylko admin mo\u017Ce tworzy\u0107 grupy" }, 403);
        const body = await request.json();
        if (!body.name_pl) return json({ error: "Brak nazwy grupy" }, 400);
        const result = await env.DB.prepare(
          "INSERT INTO groups (name_pl, name_en, name_cz) VALUES (?, ?, ?)"
        ).bind(body.name_pl, body.name_en || body.name_pl, body.name_cz || body.name_pl).run();
        return json({ ok: true, group_id: result.meta.last_row_id });
      }
      if (url.pathname === "/trainers" && request.method === "GET") {
        const user = await getUserFromToken(request, env);
        if (!user) return json({ error: "Nie zalogowano" }, 401);
        if (user.role === "student") return json({ error: "Brak uprawnie\u0144" }, 403);
        const { results } = await env.DB.prepare(
          `SELECT id, name, email FROM users WHERE role IN ('trainer','admin') AND active = 1 ORDER BY name`
        ).all();
        return json({ ok: true, trainers: results });
      }
      const groupTrainerMatch = url.pathname.match(/^\/groups\/(\d+)\/trainers$/);
      if (groupTrainerMatch && request.method === "POST") {
        const user = await getUserFromToken(request, env);
        if (!user) return json({ error: "Nie zalogowano" }, 401);
        if (user.role !== "admin") return json({ error: "Tylko admin mo\u017Ce zarz\u0105dza\u0107 przypisaniami trener\xF3w" }, 403);
        const body = await request.json();
        if (!body.trainer_id) return json({ error: "Brak trainer_id" }, 400);
        await env.DB.prepare(
          "INSERT OR IGNORE INTO trainer_group_access (trainer_id, group_id) VALUES (?, ?)"
        ).bind(body.trainer_id, parseInt(groupTrainerMatch[1])).run();
        return json({ ok: true });
      }
      const removeGroupTrainerMatch = url.pathname.match(/^\/groups\/(\d+)\/trainers\/(\d+)$/);
      if (removeGroupTrainerMatch && request.method === "DELETE") {
        const user = await getUserFromToken(request, env);
        if (!user) return json({ error: "Nie zalogowano" }, 401);
        if (user.role !== "admin") return json({ error: "Tylko admin mo\u017Ce zarz\u0105dza\u0107 przypisaniami trener\xF3w" }, 403);
        await env.DB.prepare(
          "DELETE FROM trainer_group_access WHERE group_id = ? AND trainer_id = ?"
        ).bind(parseInt(removeGroupTrainerMatch[1]), parseInt(removeGroupTrainerMatch[2])).run();
        return json({ ok: true });
      }
      const deleteUserMatch = url.pathname.match(/^\/users\/(\d+)$/);
      if (deleteUserMatch && request.method === "DELETE") {
        const user = await getUserFromToken(request, env);
        if (!user) return json({ error: "Nie zalogowano" }, 401);
        if (user.role !== "admin") return json({ error: "Tylko admin mo\u017Ce usuwa\u0107 konta" }, 403);
        const targetId = parseInt(deleteUserMatch[1]);
        await env.DB.prepare("DELETE FROM sessions WHERE user_id = ?").bind(targetId).run();
        await env.DB.prepare("DELETE FROM trainer_group_access WHERE trainer_id = ?").bind(targetId).run();
        await env.DB.prepare("UPDATE users SET active = 0 WHERE id = ?").bind(targetId).run();
        return json({ ok: true });
      }
      const updateStudentMatch = url.pathname.match(/^\/students\/(\d+)$/);
      if (updateStudentMatch && request.method === "PUT") {
        const user = await getUserFromToken(request, env);
        if (!user) return json({ error: "Nie zalogowano" }, 401);
        if (user.role === "student") return json({ error: "Brak uprawnie\u0144" }, 403);
        return await handleUpdateStudent(request, env, user, parseInt(updateStudentMatch[1]));
      }
      if (url.pathname === "/admin/update-profile" && request.method === "POST") {
        return await handleUpdateProfileByEmail(request, env);
      }
      if (url.pathname === "/admin/import-ratings" && request.method === "POST") {
        return await handleImportRatings(request, env);
      }
      if (url.pathname === "/admin/import-ratings-bulk" && request.method === "POST") {
        return await handleImportRatingsBulk(request, env);
      }
      if (url.pathname === "/admin/incremental-import-recordings" && request.method === "POST") {
        return await handleIncrementalImportRecordings(request, env);
      }
      if (url.pathname === "/admin/incremental-import-diary" && request.method === "POST") {
        return await handleIncrementalImportDiary(request, env);
      }
      if (url.pathname === "/admin/incremental-import-ratings" && request.method === "POST") {
        return await handleIncrementalImportRatings(request, env);
      }
      if (url.pathname === "/admin/import-recordings-bulk" && request.method === "POST") {
        return await handleImportRecordingsBulk(request, env);
      }
      if (url.pathname === "/admin/set-requirements-bulk" && request.method === "POST") {
        return await handleSetRequirementsBulk(request, env);
      }
      if (url.pathname === "/admin/import-diary-bulk" && request.method === "POST") {
        return await handleImportDiaryBulk(request, env);
      }
      if (url.pathname === "/admin/import-recording-meta" && request.method === "POST") {
        return await handleImportRecordingMeta(request, env);
      }
      if (url.pathname === "/admin/import-recording" && request.method === "POST") {
        return await handleImportRecording(request, env);
      }
      if (url.pathname === "/admin/import-diary" && request.method === "POST") {
        return await handleImportDiary(request, env);
      }
      const reqEditMatch = url.pathname.match(/^\/requirements\/(\d+)$/);
      if (reqEditMatch && request.method === "PUT") {
        const user = await getUserFromToken(request, env);
        if (!user) return json({ error: "Nie zalogowano" }, 401);
        if (user.role === "student") return json({ error: "Brak uprawnie\u0144" }, 403);
        const body = await request.json();
        if (!body.required_count) return json({ error: "Brak required_count" }, 400);
        await env.DB.prepare("UPDATE requirements SET required_count = ? WHERE id = ?").bind(body.required_count, parseInt(reqEditMatch[1])).run();
        return json({ ok: true });
      }
      if (reqEditMatch && request.method === "DELETE") {
        const user = await getUserFromToken(request, env);
        if (!user) return json({ error: "Nie zalogowano" }, 401);
        if (user.role === "student") return json({ error: "Brak uprawnie\u0144" }, 403);
        await env.DB.prepare("DELETE FROM requirements WHERE id = ?").bind(parseInt(reqEditMatch[1])).run();
        return json({ ok: true });
      }
      if (url.pathname === "/requirements/period" && request.method === "DELETE") {
        const user = await getUserFromToken(request, env);
        if (!user) return json({ error: "Nie zalogowano" }, 401);
        if (user.role === "student") return json({ error: "Brak uprawnie\u0144" }, 403);
        const body = await request.json();
        const { user_id, period_start, period_end } = body;
        if (!user_id || period_start === void 0 || period_end === void 0) {
          return json({ error: "Brak user_id, period_start lub period_end" }, 400);
        }
        const result = await env.DB.prepare(
          "DELETE FROM requirements WHERE user_id = ? AND period_start = ? AND period_end = ?"
        ).bind(user_id, period_start, period_end).run();
        return json({ ok: true, deleted: result.meta.changes });
      }
      if (url.pathname === "/requirements/period" && request.method === "PUT") {
        const user = await getUserFromToken(request, env);
        if (!user) return json({ error: "Nie zalogowano" }, 401);
        if (user.role === "student") return json({ error: "Brak uprawnie\u0144" }, 403);
        const body = await request.json();
        const { user_id, old_period_start, old_period_end, new_period_start, new_period_end } = body;
        if (!user_id || old_period_start === void 0 || old_period_end === void 0 || new_period_start === void 0 || new_period_end === void 0) {
          return json({ error: "Brak wymaganych p\xF3l dat" }, 400);
        }
        const result = await env.DB.prepare(
          "UPDATE requirements SET period_start = ?, period_end = ? WHERE user_id = ? AND period_start = ? AND period_end = ?"
        ).bind(new_period_start, new_period_end, user_id, old_period_start, old_period_end).run();
        return json({ ok: true, updated: result.meta.changes });
      }
      const diaryReadMatch = url.pathname.match(/^\/diary\/(\d+)\/mark-read$/);
      if (diaryReadMatch && request.method === "PUT") {
        const user = await getUserFromToken(request, env);
        if (!user) return json({ error: "Nie zalogowano" }, 401);
        if (user.role === "student") return json({ error: "Brak uprawnie\u0144" }, 403);
        await env.DB.prepare(
          "UPDATE diary_entries SET trainer_read_at = ? WHERE id = ?"
        ).bind(Math.floor(Date.now() / 1e3), parseInt(diaryReadMatch[1])).run();
        return json({ ok: true });
      }
      if (url.pathname === "/admin/fetch-recording-audio" && request.method === "POST") {
        const user = await getUserFromToken(request, env);
        if (!user) return json({ error: "Nie zalogowano" }, 401);
        if (user.role === "student") return json({ error: "Brak uprawnie\u0144" }, 403);
        return await handleFetchRecordingAudio(request, env);
      }
      if (url.pathname === "/admin/resync-student-schedule" && request.method === "POST") {
        const user = await getUserFromToken(request, env);
        if (!user) return json({ error: "Nie zalogowano" }, 401);
        if (user.role === "student") return json({ error: "Brak uprawnie\u0144" }, 403);
        return await handleResyncStudentSchedule(request, env);
      }
      if (url.pathname === "/admin/ns-auto-login" && request.method === "POST") {
        const user = await getUserFromToken(request, env);
        if (!user) return json({ error: "Nie zalogowano" }, 401);
        return await handleNsAutoLogin(request, env);
      }
      if (url.pathname === "/admin/ns-list-students" && request.method === "POST") {
        const user = await getUserFromToken(request, env);
        if (!user) return json({ error: "Nie zalogowano" }, 401);
        return await handleNsListStudents(request, env);
      }
      if (url.pathname === "/admin/create-student-from-ns" && request.method === "POST") {
        const user = await getUserFromToken(request, env);
        if (!user) return json({ error: "Nie zalogowano" }, 401);
        return await handleCreateStudentFromNs(request, env);
      }
      if (url.pathname === "/admin/review-queue" && request.method === "GET") {
        const user = await getUserFromToken(request, env);
        if (!user) return json({ error: "Nie zalogowano" }, 401);
        return await handleReviewQueue(env, user);
      }
      if (url.pathname === "/admin/sync-student-recordings" && request.method === "POST") {
        const user = await getUserFromToken(request, env);
        if (!user) return json({ error: "Nie zalogowano" }, 401);
        return await handleSyncStudentRecordings(request, env);
      }
      if (url.pathname === "/admin/gps-points" && request.method === "POST") {
        const user = await getUserFromToken(request, env);
        if (!user) return json({ error: "Nie zalogowano" }, 401);
        return await handleGetGpsPoints(request, env);
      }
      if (url.pathname === "/admin/poll-postgres-changes" && request.method === "POST") {
        const user = await getUserFromToken(request, env);
        if (!user) return json({ error: "Nie zalogowano" }, 401);
        if (user.role === "student") return json({ error: "Brak uprawnie\u0144" }, 403);
        const result = await pollPostgresChanges(env);
        return json({ ok: true, ...result });
      }
      const dayDetailMatch = url.pathname.match(/^\/students\/(\d+)\/day\/(\d{4}-\d{2}-\d{2})$/);
      if (dayDetailMatch && request.method === "GET") {
        const user = await getUserFromToken(request, env);
        if (!user) return json({ error: "Nie zalogowano" }, 401);
        if (user.role === "student") return json({ error: "Brak uprawnie\u0144" }, 403);
        return await handleStudentDay(env, user, parseInt(dayDetailMatch[1]), dayDetailMatch[2]);
      }
      const statsMatch = url.pathname.match(/^\/students\/(\d+)\/stats$/);
      if (statsMatch && request.method === "GET") {
        const user = await getUserFromToken(request, env);
        if (!user) return json({ error: "Nie zalogowano" }, 401);
        if (user.role === "student") return json({ error: "Brak uprawnie\u0144" }, 403);
        return await handleStudentStats(env, user, parseInt(statsMatch[1]));
      }
      return json({ error: "Nie znaleziono endpointu" }, 404);
    } catch (e) {
      try {
        const u = new URL(request.url);
        console.error("BLAD 500", u.pathname, u.searchParams.get("ns_email") || "", e && e.message, e && e.stack ? String(e.stack).split("\n").slice(0, 4).join(" | ") : "");
      } catch (x) {
      }
      return json({ error: "B\u0142\u0105d serwera: " + e.message }, 500);
    }
  },
  // ── CYKLICZNE SPRAWDZANIE ZMIAN W POSTGRESIE NS (Cron Trigger, co 1 minutę) ──
  // Postgres nie może "wypychać" zmian do Workera (Workers nie trzymają stałych
  // połączeń), więc zamiast tego co minutę pytamy: co zmieniło się od ostatniego
  // sprawdzenia (po kolumnach updated_at/created_at). Jeśli coś się zmieniło dla
  // danego kursanta, synchronizujemy tylko jego (nie wszystkich).
  async scheduled(event, env, ctx) {
    ctx.waitUntil(pollPostgresChanges(env));
    ctx.waitUntil(transcribeTick(env).catch(() => {
    }));
  }
};
async function handleSetStudentSetup(request, env) {
  const body = await request.json();
  const { student_id, ns_token, ns_email, ns_server, group_id, max_records_count, records_permission, diary_permission } = body;
  if (!student_id) return json({ error: "Brak student_id" }, 400);
  const valid = await validateNsToken(ns_token, ns_email, ns_server);
  if (!valid) return json({ error: "Nieprawid\u0142owy lub wygas\u0142y token NS \u2014 zaloguj si\u0119 ponownie" }, 401);
  const allowedManage = await hasTrainerPermission(env, ns_email, "can_manage_students");
  if (!allowedManage) return json({ error: "Brak uprawnie\u0144 do zarz\u0105dzania kursantami" }, 403);
  const pgClient = new Client({ connectionString: env.HYPERDRIVE_STUDENT_SETUP.connectionString });
  await pgClient.connect();
  try {
    if (group_id !== void 0) {
      await pgClient.query("UPDATE users SET students_group_id = $1 WHERE id = $2", [group_id || null, student_id]);
    }
    await pgClient.query(
      "UPDATE user_permissions SET max_records_count = $1, records_permission = $2, diary_permission = $3 WHERE user_id = $4",
      [max_records_count != null ? max_records_count : 20, records_permission !== false, diary_permission !== false, student_id]
    );
    return json({ ok: true });
  } catch (e) {
    return json({ error: "B\u0142\u0105d zapisu: " + e.message }, 500);
  } finally {
    await pgClient.end();
  }
}
__name(handleSetStudentSetup, "handleSetStudentSetup");
__name2(handleSetStudentSetup, "handleSetStudentSetup");
async function handleForceDeleteRecord(request, env) {
  const body = await request.json();
  const { record_id, ns_token, ns_email, ns_server } = body;
  if (!record_id) return json({ error: "Brak record_id" }, 400);
  const valid = await validateNsToken(ns_token, ns_email, ns_server);
  if (!valid) return json({ error: "Nieprawid\u0142owy lub wygas\u0142y token NS \u2014 zaloguj si\u0119 ponownie" }, 401);
  const allowedManage = await hasTrainerPermission(env, ns_email, "can_manage_students");
  if (!allowedManage) return json({ error: "Brak uprawnie\u0144 do zarz\u0105dzania kursantami" }, 403);
  const pgClient = new Client({ connectionString: env.HYPERDRIVE_FORCE_DELETE.connectionString });
  await pgClient.connect();
  try {
    await pgClient.query("DELETE FROM record_rates WHERE record_id = $1", [record_id]);
    const result = await pgClient.query("DELETE FROM records WHERE id = $1", [record_id]);
    return json({ ok: true, deleted: result.rowCount });
  } catch (e) {
    return json({ error: "B\u0142\u0105d usuwania: " + e.message }, 500);
  } finally {
    await pgClient.end();
  }
}
__name(handleForceDeleteRecord, "handleForceDeleteRecord");
__name2(handleForceDeleteRecord, "handleForceDeleteRecord");
function nsServerBase(serverChoice) {
  if (serverChoice === "old") return "http://newspeech.nazwa.pl";
  return "https://ns.nowamowa.com";
}
__name(nsServerBase, "nsServerBase");
__name2(nsServerBase, "nsServerBase");
async function handleNsProxy(request, url, env) {
  const serverChoice = request.headers.get("X-NS-Server") || url.searchParams.get("ns_server");
  const base = nsServerBase(serverChoice);
  const targetPath = url.pathname.replace("/ns-proxy", "") + url.search;
  const targetUrl = base + targetPath;
  const emailHeader = request.headers.get("Email");
  const nsPath = url.pathname.replace("/ns-proxy", "");
  const isRecordsGet = request.method === "GET" && /^\/api\/v1\/records(\/\d+)?(\?|$)/.test(nsPath);
  const isReviewWrite = request.method === "PUT" && /^\/api\/v1\/records\/\d+\/review$/.test(nsPath);
  if (isRecordsGet) {
    const allowed = await hasTrainerPermission(env, emailHeader, "can_view_recordings");
    if (!allowed) return json({ error: "Brak uprawnie\u0144 do podgl\u0105du nagra\u0144" }, 403);
  }
  if (isReviewWrite) {
    const allowed = await hasTrainerPermission(env, emailHeader, "can_add_reviews");
    if (!allowed) return json({ error: "Brak uprawnie\u0144 do dodawania ocen" }, 403);
  }
  const forwardHeaders = {};
  ["Authorization", "Email", "Content-Type", "Accept", "X-Requested-With"].forEach((h) => {
    const v = request.headers.get(h);
    if (v) forwardHeaders[h] = v;
  });
  forwardHeaders["Origin"] = base;
  forwardHeaders["Referer"] = base + "/front/index.html";
  const init = { method: request.method, headers: forwardHeaders };
  if (!["GET", "HEAD"].includes(request.method)) {
    init.body = await request.text();
  }
  const nsResp = await fetch(targetUrl, init);
  const bodyText = await nsResp.text();
  const respHeaders = new Headers(CORS);
  respHeaders.set("Content-Type", nsResp.headers.get("Content-Type") || "application/json");
  return new Response(bodyText, { status: nsResp.status, headers: respHeaders });
}
__name(handleNsProxy, "handleNsProxy");
__name2(handleNsProxy, "handleNsProxy");
async function handleAudioProxy(request, env) {
  const url = new URL(request.url);
  const targetPath = url.searchParams.get("path");
  if (!targetPath || !targetPath.startsWith("/uploads/")) {
    return json({ error: "Nieprawid\u0142owa \u015Bcie\u017Cka pliku" }, 400);
  }
  const serverChoice = request.headers.get("X-NS-Server") || url.searchParams.get("ns_server");
  const base = nsServerBase(serverChoice);
  const authHeader = request.headers.get("X-NS-Auth") || (url.searchParams.get("ns_token") ? "Token token=" + url.searchParams.get("ns_token") : null);
  const emailHeader = request.headers.get("X-NS-Email") || url.searchParams.get("ns_email");
  const fetchHeaders = {};
  if (authHeader) fetchHeaders["Authorization"] = authHeader;
  if (emailHeader) fetchHeaders["Email"] = emailHeader;
  if (emailHeader) {
    const allowed = await hasTrainerPermission(env, emailHeader, "can_listen_recordings");
    if (!allowed) return json({ error: "Brak uprawnie\u0144 do ods\u0142uchu nagra\u0144" }, 403);
  }
  const nsResp = await fetch(base + targetPath, { headers: fetchHeaders });
  if (!nsResp.ok) {
    return new Response("B\u0142\u0105d NS: " + nsResp.status, { status: 502, headers: { ...CORS } });
  }
  const respHeaders = new Headers(CORS);
  respHeaders.set("Content-Type", nsResp.headers.get("Content-Type") || "audio/mpeg");
  respHeaders.set("Cache-Control", "public, max-age=3600");
  if (url.searchParams.get("download") === "1") {
    const filename = targetPath.split("/").pop() || "nagranie.mp3";
    respHeaders.set("Content-Disposition", 'attachment; filename="' + filename.replace(/"/g, "") + '"');
  }
  return new Response(nsResp.body, { status: 200, headers: respHeaders });
}
__name(handleAudioProxy, "handleAudioProxy");
__name2(handleAudioProxy, "handleAudioProxy");
var SPEED_ENV = null;
var SPEED_CTX = null;
var SPEED_TABLES_READY = false;
var TOKEN_MEM = /* @__PURE__ */ new Map();
async function speedTables(env) {
  if (SPEED_TABLES_READY || !env || !env.DB) return;
  await env.DB.batch([
    env.DB.prepare("CREATE TABLE IF NOT EXISTS ns_token_ok (k TEXT PRIMARY KEY, email TEXT, until INTEGER)"),
    env.DB.prepare("CREATE TABLE IF NOT EXISTS ns_user_cache (email TEXT PRIMARY KEY, ns_id TEXT, role TEXT, at INTEGER)"),
    env.DB.prepare("CREATE TABLE IF NOT EXISTS harmo_stats_cache (k TEXT PRIMARY KEY, email TEXT, body TEXT, at INTEGER)"),
    env.DB.prepare("CREATE INDEX IF NOT EXISTS idx_harmo_stats_email ON harmo_stats_cache(email)")
  ]);
  SPEED_TABLES_READY = true;
}
__name(speedTables, "speedTables");
async function tokenKey(nsToken, nsEmail, serverChoice) {
  const d = await crypto.subtle.digest("SHA-256", new TextEncoder().encode((serverChoice === "old" ? "old" : "new") + "|" + String(nsEmail).toLowerCase() + "|" + nsToken));
  return [...new Uint8Array(d)].map((b) => b.toString(16).padStart(2, "0")).join("");
}
__name(tokenKey, "tokenKey");
function speedLater(promise) {
  try {
    if (SPEED_CTX && SPEED_CTX.waitUntil) {
      SPEED_CTX.waitUntil(promise);
      return;
    }
  } catch (e) {
  }
  promise.catch(() => {
  });
}
__name(speedLater, "speedLater");
async function validateNsToken(nsToken, nsEmail, serverChoice) {
  if (!nsToken || !nsEmail) return false;
  const now = Math.floor(Date.now() / 1e3);
  let k = null;
  try {
    k = await tokenKey(nsToken, nsEmail, serverChoice);
    const m = TOKEN_MEM.get(k);
    if (m && m > now) return true;
    if (SPEED_ENV && SPEED_ENV.DB) {
      await speedTables(SPEED_ENV);
      const row = await SPEED_ENV.DB.prepare("SELECT until FROM ns_token_ok WHERE k = ?").bind(k).first();
      if (row && row.until > now) {
        TOKEN_MEM.set(k, Math.min(row.until, now + 600));
        return true;
      }
    }
  } catch (e) {
  }
  const ok = await validateNsTokenRemote(nsToken, nsEmail, serverChoice);
  if (ok && k) {
    TOKEN_MEM.set(k, now + 600);
    if (TOKEN_MEM.size > 5e3) TOKEN_MEM.clear();
    if (SPEED_ENV && SPEED_ENV.DB) speedLater(SPEED_ENV.DB.prepare("INSERT INTO ns_token_ok (k, email, until) VALUES (?, ?, ?) ON CONFLICT(k) DO UPDATE SET until = excluded.until").bind(k, String(nsEmail).toLowerCase(), now + 1800).run());
  }
  return ok;
}
__name(validateNsToken, "validateNsToken");
__name2(validateNsToken, "validateNsToken");
async function validateNsTokenRemote(nsToken, nsEmail, serverChoice) {
  if (!nsToken || !nsEmail) return false;
  try {
    const r = await fetch(nsServerBase(serverChoice) + "/api/v1/record_categories", {
      headers: {
        "Accept": "application/json",
        "Authorization": `Token token=${nsToken}`,
        "Email": nsEmail,
        "X-Requested-With": "XMLHttpRequest"
      }
    });
    return r.ok;
  } catch (e) {
    return false;
  }
}
__name(validateNsTokenRemote, "validateNsTokenRemote");
__name2(validateNsTokenRemote, "validateNsTokenRemote");
async function getOrCreateUserByNsEmail(env, nsEmail, nsName) {
  let user = await env.DB.prepare("SELECT * FROM users WHERE email = ?").bind(nsEmail).first();
  if (user) return user;
  const result = await env.DB.prepare(
    `INSERT INTO users (email, password_hash, role, name, active, created_at)
     VALUES (?, ?, 'student', ?, 1, unixepoch())`
  ).bind(nsEmail, "NS_BRIDGE_NO_LOCAL_LOGIN", nsName || nsEmail).run();
  return await env.DB.prepare("SELECT * FROM users WHERE id = ?").bind(result.meta.last_row_id).first();
}
__name(getOrCreateUserByNsEmail, "getOrCreateUserByNsEmail");
__name2(getOrCreateUserByNsEmail, "getOrCreateUserByNsEmail");
async function requireNsBridgeUser(request, env) {
  const url = new URL(request.url);
  let nsToken = url.searchParams.get("ns_token");
  let nsEmail = url.searchParams.get("ns_email");
  let nsServer = url.searchParams.get("ns_server");
  let nsName = url.searchParams.get("ns_name");
  if (!nsToken || !nsEmail) {
    try {
      const body = await request.clone().json();
      nsToken = nsToken || body.ns_token;
      nsEmail = nsEmail || body.ns_email;
      nsServer = nsServer || body.ns_server;
      nsName = nsName || body.ns_name;
    } catch (e) {
    }
  }
  const valid = await validateNsToken(nsToken, nsEmail, nsServer);
  if (!valid) return null;
  return await getOrCreateUserByNsEmail(env, nsEmail, nsName);
}
__name(requireNsBridgeUser, "requireNsBridgeUser");
__name2(requireNsBridgeUser, "requireNsBridgeUser");
function isNotCountedReview(details) {
  return /^\s*🚫/.test(String(details || ""));
}
__name(isNotCountedReview, "isNotCountedReview");
__name2(isNotCountedReview, "isNotCountedReview");
function correctionEntryOut(r) {
  return {
    id: r.ns_record_id,
    category_name: r.category_name,
    recorded_at: r.recorded_at ?? null,
    first_sent_at: r.first_sent_at ?? null,
    record_date: r.record_date ?? null,
    reviewed_at: r.reviewed_at,
    weak_areas: JSON.parse(r.weak_areas || "[]"),
    all_good: !!r.all_good,
    review_details: r.review_details ?? null,
    not_counted: isNotCountedReview(r.review_details)
  };
}
__name(correctionEntryOut, "correctionEntryOut");
__name2(correctionEntryOut, "correctionEntryOut");
var NC_DETAILS_COL_READY = false;
async function ensureNcDetailsColumn(env) {
  if (NC_DETAILS_COL_READY) return;
  try {
    await env.DB.prepare("SELECT review_details FROM needs_correction_cache LIMIT 1").first();
  } catch (e) {
    try {
      await env.DB.prepare("ALTER TABLE needs_correction_cache ADD COLUMN review_details TEXT").run();
    } catch (e2) {
    }
  }
  NC_DETAILS_COL_READY = true;
}
__name(ensureNcDetailsColumn, "ensureNcDetailsColumn");
__name2(ensureNcDetailsColumn, "ensureNcDetailsColumn");
async function syncNeedsCorrectionFromPostgres(env, localUserId, nsEmail) {
  await ensureNcDetailsColumn(env);
  const pgClient = new Client({ connectionString: env.HYPERDRIVE.connectionString });
  await pgClient.connect();
  try {
    const result = await pgClient.query(
      `SELECT
         r.id AS record_id,
         r.reviewed_at,
         r.review_details,
         r.date AS record_date,
         r.created_at AS first_sent_at,
         GREATEST(r.created_at, COALESCE(r.updated_by_author_at, r.created_at)) AS recorded_at,
         rc_t.name AS category_name,
         rrc_t.name AS rate_category_name,
         ru.percent_mark,
         ru_t.name AS unit_name
       FROM records r
       JOIN users u ON u.id = r.user_id
       JOIN record_categories rc ON rc.id = r.record_category_id
       LEFT JOIN record_category_translations rc_t ON rc_t.record_category_id = rc.id AND rc_t.locale = 'pl'
       LEFT JOIN record_rates rr ON rr.record_id = r.id
       LEFT JOIN record_rate_categories rrc ON rrc.id = rr.record_rate_category_id
       LEFT JOIN record_rate_category_translations rrc_t ON rrc_t.record_rate_category_id = rrc.id AND rrc_t.locale = 'pl'
       LEFT JOIN record_rate_units ru ON ru.id = rr.record_rate_unit_id
       LEFT JOIN record_rate_unit_translations ru_t ON ru_t.record_rate_unit_id = ru.id AND ru_t.locale = 'pl'
       WHERE u.email = $1 AND r.is_correct = false AND r.reviewed_at IS NOT NULL AND r.deleted_at IS NULL
       ORDER BY r.reviewed_at DESC
       LIMIT 2000`,
      [nsEmail]
    );
    const byRecord = /* @__PURE__ */ new Map();
    for (const row of result.rows) {
      if (!byRecord.has(row.record_id)) {
        byRecord.set(row.record_id, {
          ns_record_id: row.record_id,
          category_name: row.category_name,
          reviewed_at: row.reviewed_at,
          review_details: row.review_details || null,
          record_date: row.record_date,
          first_sent_at: row.first_sent_at,
          recorded_at: row.recorded_at,
          weak_areas: []
        });
      }
      const wkList = byRecord.get(row.record_id).weak_areas;
      if (row.rate_category_name && wkList.some((w) => String(w.category_name).toLowerCase() === String(row.rate_category_name).toLowerCase())) continue;
      if (row.rate_category_name && row.percent_mark !== null && row.percent_mark < 100) {
        byRecord.get(row.record_id).weak_areas.push({
          category_name: row.rate_category_name,
          percent_mark: row.percent_mark,
          unit_name: row.unit_name
        });
      }
    }
    const ncKey = /* @__PURE__ */ __name((id, cat, rv, rc, fs, rd, wa, det) => [id, cat, rv, rc, fs, rd, wa, det].map((x) => x == null ? "" : String(x)).join("|"), "ncKey");
    const newSig = [...byRecord.values()].map((rec) => {
      const rd = rec.record_date instanceof Date ? rec.record_date.toISOString().slice(0, 10) : rec.record_date ? String(rec.record_date).slice(0, 10) : null;
      const ts = /* @__PURE__ */ __name((v) => v ? Math.floor(new Date(v).getTime() / 1e3) : null, "ts");
      return ncKey(rec.ns_record_id, rec.category_name, ts(rec.reviewed_at), ts(rec.recorded_at), ts(rec.first_sent_at), rd, JSON.stringify(rec.weak_areas), rec.review_details);
    }).sort().join(";");
    const { results: oldRows } = await env.DB.prepare(
      "SELECT ns_record_id, category_name, reviewed_at, recorded_at, first_sent_at, record_date, weak_areas, review_details FROM needs_correction_cache WHERE user_id = ?"
    ).bind(localUserId).all();
    const oldSig = (oldRows || []).map((r) => ncKey(r.ns_record_id, r.category_name, r.reviewed_at, r.recorded_at, r.first_sent_at, r.record_date, r.weak_areas, r.review_details)).sort().join(";");
    const ncUnchanged = newSig === oldSig;
    const stmts = ncUnchanged ? [] : [env.DB.prepare("DELETE FROM needs_correction_cache WHERE user_id = ?").bind(localUserId)];
    for (const rec of ncUnchanged ? [] : byRecord.values()) {
      const reviewedTs = rec.reviewed_at ? Math.floor(new Date(rec.reviewed_at).getTime() / 1e3) : null;
      const recordedTs = rec.recorded_at ? Math.floor(new Date(rec.recorded_at).getTime() / 1e3) : null;
      const firstSentTs = rec.first_sent_at ? Math.floor(new Date(rec.first_sent_at).getTime() / 1e3) : null;
      const recordDate = rec.record_date instanceof Date ? rec.record_date.toISOString().slice(0, 10) : rec.record_date ? String(rec.record_date).slice(0, 10) : null;
      stmts.push(env.DB.prepare(
        `INSERT INTO needs_correction_cache (user_id, ns_record_id, category_name, reviewed_at, recorded_at, first_sent_at, record_date, weak_areas, all_good, synced_at, review_details)
         VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
         ON CONFLICT(ns_record_id) DO UPDATE SET
           category_name=excluded.category_name, reviewed_at=excluded.reviewed_at,
           recorded_at=excluded.recorded_at, first_sent_at=excluded.first_sent_at, record_date=excluded.record_date,
           weak_areas=excluded.weak_areas, all_good=excluded.all_good, synced_at=excluded.synced_at, review_details=excluded.review_details`
      ).bind(localUserId, rec.ns_record_id, rec.category_name, reviewedTs, recordedTs, firstSentTs, recordDate, JSON.stringify(rec.weak_areas), rec.weak_areas.length === 0 && !isNotCountedReview(rec.review_details) ? 1 : 0, Math.floor(Date.now() / 1e3), rec.review_details || null));
    }
    if (stmts.length) {
      const BATCH = 30;
      for (let i = 0; i < stmts.length; i += BATCH) {
        await env.DB.batch(stmts.slice(i, i + BATCH));
      }
    }
    await env.DB.prepare(
      `INSERT INTO needs_correction_sync_state (user_id, last_synced_at) VALUES (?, ?)
       ON CONFLICT(user_id) DO UPDATE SET last_synced_at=excluded.last_synced_at`
    ).bind(localUserId, Math.floor(Date.now() / 1e3)).run();
    return byRecord.size;
  } finally {
    try {
      await pgClient.end();
    } catch (e) {
    }
  }
}
__name(syncNeedsCorrectionFromPostgres, "syncNeedsCorrectionFromPostgres");
__name2(syncNeedsCorrectionFromPostgres, "syncNeedsCorrectionFromPostgres");
async function harmoStatsCached(env, nsEmail, dateFrom, dateTo) {
  const em = String(nsEmail).toLowerCase(), k = em + "|" + dateFrom + "|" + dateTo, now = Math.floor(Date.now() / 1e3);
  let row = null;
  try {
    await speedTables(env);
    row = await env.DB.prepare("SELECT body, at FROM harmo_stats_cache WHERE k = ?").bind(k).first();
  } catch (e) {
  }
  const recompute = /* @__PURE__ */ __name(async () => {
    const st = await fetchHarmonogramStatsFromPostgres(env, nsEmail, dateFrom, dateTo);
    try {
      await env.DB.prepare("INSERT INTO harmo_stats_cache (k, email, body, at) VALUES (?, ?, ?, ?) ON CONFLICT(k) DO UPDATE SET body = excluded.body, at = excluded.at").bind(k, em, JSON.stringify(st), Math.floor(Date.now() / 1e3)).run();
    } catch (e) {
    }
    return st;
  }, "recompute");
  if (row) {
    let st = null;
    try {
      st = JSON.parse(row.body);
    } catch (e) {
    }
    if (st) {
      if (row.at < now - 600) speedLater(recompute());
      return st;
    }
  }
  return await recompute();
}
__name(harmoStatsCached, "harmoStatsCached");
__name2(harmoStatsCached, "harmoStatsCached");
async function fetchHarmonogramStatsFromPostgres(env, nsEmail, dateFrom, dateTo) {
  const pgClient = new Client({ connectionString: env.HYPERDRIVE.connectionString });
  await pgClient.connect();
  try {
    const overallResult = await pgClient.query(
      `SELECT
         COUNT(*) AS sent,
         COUNT(*) FILTER (WHERE r.reviewed_at IS NOT NULL) AS reviewed,
         COUNT(*) FILTER (WHERE r.is_correct = true AND r.reviewed_at IS NOT NULL) AS correct
       FROM records r
       JOIN users u ON u.id = r.user_id
       WHERE u.email = $1 AND r.deleted_at IS NULL AND r.created_at >= $2 AND r.created_at < ($3::date + interval '1 day')
         AND COALESCE(r.review_details, '') NOT LIKE '\u{1F6AB}%'`,
      [nsEmail, dateFrom, dateTo]
    );
    const overall = overallResult.rows[0] || { sent: 0, reviewed: 0, correct: 0 };
    const byCategoryResult = await pgClient.query(
      `SELECT
         r.record_category_id AS category_id,
         rc_t.name AS category_name,
         COUNT(*) AS sent,
         COUNT(*) FILTER (WHERE r.reviewed_at IS NOT NULL) AS reviewed,
         COUNT(*) FILTER (WHERE r.is_correct = true AND r.reviewed_at IS NOT NULL) AS correct
       FROM records r
       JOIN users u ON u.id = r.user_id
       LEFT JOIN record_categories rc ON rc.id = r.record_category_id
       LEFT JOIN record_category_translations rc_t ON rc_t.record_category_id = rc.id AND rc_t.locale = 'pl'
       WHERE u.email = $1 AND r.deleted_at IS NULL AND r.created_at >= $2 AND r.created_at < ($3::date + interval '1 day')
         AND COALESCE(r.review_details, '') NOT LIKE '\u{1F6AB}%'
       GROUP BY r.record_category_id, rc_t.name`,
      [nsEmail, dateFrom, dateTo]
    );
    const byNameTotals = {};
    for (const row of byCategoryResult.rows) {
      const name = row.category_name || row.category_id;
      if (!byNameTotals[name]) byNameTotals[name] = { sent: 0, correct: 0, reviewed: 0, ids: [] };
      byNameTotals[name].sent += parseInt(row.sent, 10);
      byNameTotals[name].correct += parseInt(row.correct, 10);
      byNameTotals[name].reviewed += parseInt(row.reviewed, 10);
      byNameTotals[name].ids.push(row.category_id);
    }
    const byCategory = {};
    for (const name of Object.keys(byNameTotals)) {
      const totals = byNameTotals[name];
      for (const id of totals.ids) {
        byCategory[id] = { category_name: name, sent: totals.sent, correct: totals.correct, reviewed: totals.reviewed };
      }
    }
    return {
      overall: { sent: parseInt(overall.sent, 10) || 0, reviewed: parseInt(overall.reviewed, 10) || 0, correct: parseInt(overall.correct, 10) || 0 },
      byCategory
    };
  } finally {
    try {
      await pgClient.end();
    } catch (e) {
    }
  }
}
__name(fetchHarmonogramStatsFromPostgres, "fetchHarmonogramStatsFromPostgres");
__name2(fetchHarmonogramStatsFromPostgres, "fetchHarmonogramStatsFromPostgres");
var POLISH_VOIVODESHIPS = [
  { name: "Dolno\u015Bl\u0105skie", lat: 51.1, lng: 16.9 },
  { name: "Kujawsko-Pomorskie", lat: 53.1, lng: 18.4 },
  { name: "Lubelskie", lat: 51.1, lng: 22.9 },
  { name: "Lubuskie", lat: 52.2, lng: 15.3 },
  { name: "\u0141\xF3dzkie", lat: 51.6, lng: 19.5 },
  { name: "Ma\u0142opolskie", lat: 49.9, lng: 20.3 },
  { name: "Mazowieckie", lat: 52.4, lng: 21 },
  { name: "Opolskie", lat: 50.6, lng: 17.9 },
  { name: "Podkarpackie", lat: 50, lng: 22.2 },
  { name: "Podlaskie", lat: 53.3, lng: 23 },
  { name: "Pomorskie", lat: 54.2, lng: 18.2 },
  { name: "\u015Al\u0105skie", lat: 50.3, lng: 19 },
  { name: "\u015Awi\u0119tokrzyskie", lat: 50.8, lng: 20.9 },
  { name: "Warmi\u0144sko-Mazurskie", lat: 53.9, lng: 20.7 },
  { name: "Wielkopolskie", lat: 52.3, lng: 17.1 },
  { name: "Zachodniopomorskie", lat: 53.5, lng: 15.3 }
];
function nearestVoivodeship(lat, lng) {
  if (lat < 48.5 || lat > 55.5 || lng < 13 || lng > 25) return null;
  let best = null, bestDist = Infinity;
  for (const v of POLISH_VOIVODESHIPS) {
    const d = Math.pow(lat - v.lat, 2) + Math.pow(lng - v.lng, 2);
    if (d < bestDist) {
      bestDist = d;
      best = v.name;
    }
  }
  return best;
}
__name(nearestVoivodeship, "nearestVoivodeship");
__name2(nearestVoivodeship, "nearestVoivodeship");
async function handleAdminPeriodStats(request, env, url) {
  const nsToken = url.searchParams.get("ns_token");
  const nsEmail = url.searchParams.get("ns_email");
  const nsServer = url.searchParams.get("ns_server");
  const dateFrom = url.searchParams.get("date_from");
  const dateTo = url.searchParams.get("date_to");
  if (!dateFrom || !dateTo) return json({ error: "Brak date_from lub date_to" }, 400);
  const valid = await validateNsToken(nsToken, nsEmail, nsServer);
  if (!valid) return json({ error: "Nieprawid\u0142owy lub wygas\u0142y token NS" }, 401);
  const admin = await isNsAdmin(env, nsEmail);
  if (!admin) return json({ error: "Ta statystyka jest dost\u0119pna tylko dla administratora" }, 403);
  const pgClient = new Client({ connectionString: env.HYPERDRIVE.connectionString });
  await pgClient.connect();
  try {
    const studentsResult = await pgClient.query(
      `SELECT COUNT(DISTINCT r.user_id) AS cnt
       FROM records r
       WHERE r.deleted_at IS NULL AND r.created_at >= $1 AND r.created_at < ($2::date + interval '1 day')`,
      [dateFrom, dateTo]
    );
    const studentsCount = parseInt(studentsResult.rows[0]?.cnt, 10) || 0;
    const categoryResult = await pgClient.query(
      `SELECT r.record_category_id AS category_id, rc_t.name AS category_name,
              COUNT(*) AS sent, COUNT(*) FILTER (WHERE r.is_correct = false AND r.reviewed_at IS NOT NULL) AS needs_correction
       FROM records r
       LEFT JOIN record_categories rc ON rc.id = r.record_category_id
       LEFT JOIN record_category_translations rc_t ON rc_t.record_category_id = rc.id AND rc_t.locale = 'pl'
       WHERE r.deleted_at IS NULL AND r.created_at >= $1 AND r.created_at < ($2::date + interval '1 day')
       GROUP BY r.record_category_id, rc_t.name`,
      [dateFrom, dateTo]
    );
    const byNameTotals = {};
    for (const row of categoryResult.rows) {
      const name = row.category_name || row.category_id || "(bez kategorii)";
      if (!byNameTotals[name]) byNameTotals[name] = { sent: 0, needsCorrection: 0 };
      byNameTotals[name].sent += parseInt(row.sent, 10);
      byNameTotals[name].needsCorrection += parseInt(row.needs_correction, 10);
    }
    const byCategory = Object.keys(byNameTotals).map((name) => ({
      category_name: name,
      sent: byNameTotals[name].sent,
      needs_correction: byNameTotals[name].needsCorrection
    })).sort((a, b) => b.sent - a.sent);
    const totalNeedsCorrection = byCategory.reduce((sum, c) => sum + c.needs_correction, 0);
    const totalSent = byCategory.reduce((sum, c) => sum + c.sent, 0);
    const harmonogramsOverallResult = await pgClient.query(
      `SELECT COUNT(*) AS cnt FROM visit_dates vd
       WHERE vd.created_at >= $1 AND vd.created_at < ($2::date + interval '1 day')`,
      [dateFrom, dateTo]
    );
    const totalHarmonograms = parseInt(harmonogramsOverallResult.rows[0]?.cnt, 10) || 0;
    const harmonogramsByCountryResult = await pgClient.query(
      `SELECT u.country AS country, COUNT(*) AS cnt
       FROM visit_dates vd
       JOIN users u ON u.id = vd.student_id
       WHERE vd.created_at >= $1 AND vd.created_at < ($2::date + interval '1 day')
       GROUP BY u.country`,
      [dateFrom, dateTo]
    );
    const harmonogramsByCountry = {};
    for (const row of harmonogramsByCountryResult.rows) {
      harmonogramsByCountry[row.country || "\u2014"] = parseInt(row.cnt, 10) || 0;
    }
    const countryResult = await pgClient.query(
      `SELECT u.country AS country, u.id AS student_id, r.record_category_id AS category_id, rc_t.name AS category_name,
              COUNT(*) AS sent, COUNT(*) FILTER (WHERE r.is_correct = false AND r.reviewed_at IS NOT NULL) AS needs_correction
       FROM records r
       JOIN users u ON u.id = r.user_id
       LEFT JOIN record_categories rc ON rc.id = r.record_category_id
       LEFT JOIN record_category_translations rc_t ON rc_t.record_category_id = rc.id AND rc_t.locale = 'pl'
       WHERE r.deleted_at IS NULL AND r.created_at >= $1 AND r.created_at < ($2::date + interval '1 day')
       GROUP BY u.country, u.id, r.record_category_id, rc_t.name`,
      [dateFrom, dateTo]
    );
    const byCountry = {};
    for (const row of countryResult.rows) {
      const country = row.country || "\u2014";
      if (!byCountry[country]) byCountry[country] = { total: 0, needsCorrection: 0, catTotals: {}, studentIds: /* @__PURE__ */ new Set() };
      byCountry[country].studentIds.add(row.student_id);
      const name = row.category_name || row.category_id || "(bez kategorii)";
      if (!byCountry[country].catTotals[name]) byCountry[country].catTotals[name] = { sent: 0, needsCorrection: 0 };
      const sent = parseInt(row.sent, 10), nc = parseInt(row.needs_correction, 10);
      byCountry[country].catTotals[name].sent += sent;
      byCountry[country].catTotals[name].needsCorrection += nc;
      byCountry[country].total += sent;
      byCountry[country].needsCorrection += nc;
    }
    const allCountryKeys = /* @__PURE__ */ new Set([...Object.keys(byCountry), ...Object.keys(harmonogramsByCountry)]);
    const byCountryList = Array.from(allCountryKeys).map((country) => ({
      country,
      students_count: byCountry[country] ? byCountry[country].studentIds.size : 0,
      sent: byCountry[country] ? byCountry[country].total : 0,
      needs_correction: byCountry[country] ? byCountry[country].needsCorrection : 0,
      harmonograms_count: harmonogramsByCountry[country] || 0,
      by_category: byCountry[country] ? Object.keys(byCountry[country].catTotals).map((name) => ({
        category_name: name,
        sent: byCountry[country].catTotals[name].sent,
        needs_correction: byCountry[country].catTotals[name].needsCorrection
      })).sort((a, b) => b.sent - a.sent) : []
    })).sort((a, b) => b.sent - a.sent);
    const gpsResult = await pgClient.query(
      `SELECT record_file FROM records
       WHERE deleted_at IS NULL AND created_at >= $1 AND created_at < ($2::date + interval '1 day')
         AND record_file ~ '_GPS-?[0-9]+\\.[0-9]+_-?[0-9]+\\.[0-9]+'`,
      [dateFrom, dateTo]
    );
    const voivodeshipCounts = {};
    for (const row of gpsResult.rows) {
      const m = row.record_file.match(/_GPS(-?[0-9]+\.[0-9]+)_(-?[0-9]+\.[0-9]+)/);
      if (!m) continue;
      const lat = parseFloat(m[1]), lng = parseFloat(m[2]);
      const v = nearestVoivodeship(lat, lng);
      if (!v) continue;
      voivodeshipCounts[v] = (voivodeshipCounts[v] || 0) + 1;
    }
    const byVoivodeship = Object.keys(voivodeshipCounts).map((name) => ({ voivodeship: name, count: voivodeshipCounts[name] })).sort((a, b) => b.count - a.count);
    return json({
      ok: true,
      date_from: dateFrom,
      date_to: dateTo,
      students_count: studentsCount,
      total_sent: totalSent,
      total_needs_correction: totalNeedsCorrection,
      total_harmonograms: totalHarmonograms,
      by_category: byCategory,
      by_country: byCountryList,
      by_voivodeship: byVoivodeship
    });
  } catch (e) {
    return json({ error: "B\u0142\u0105d statystyk: " + e.message }, 500);
  } finally {
    try {
      await pgClient.end();
    } catch (e) {
    }
  }
}
__name(handleAdminPeriodStats, "handleAdminPeriodStats");
__name2(handleAdminPeriodStats, "handleAdminPeriodStats");
async function isNsAdmin(env, email) {
  if (!email) return false;
  const pgClient = new Client({ connectionString: env.HYPERDRIVE.connectionString });
  try {
    await pgClient.connect();
    const result = await pgClient.query(
      `SELECT role FROM users WHERE LOWER(email) = LOWER($1) AND deleted_at IS NULL LIMIT 1`,
      [email]
    );
    return result.rows.length > 0 && result.rows[0].role === "admin";
  } catch (e) {
    return false;
  } finally {
    try {
      await pgClient.end();
    } catch (e) {
    }
  }
}
__name(isNsAdmin, "isNsAdmin");
__name2(isNsAdmin, "isNsAdmin");
async function hasTrainerPermission(env, trainerEmail, permissionKey) {
  if (!trainerEmail) return true;
  const allowedKeys = ["can_view_recordings", "can_listen_recordings", "can_add_reviews", "can_add_voice_reviews", "can_manage_students"];
  if (!allowedKeys.includes(permissionKey)) return true;
  try {
    const row = await env.DB.prepare(
      `SELECT ${permissionKey} as val FROM trainer_permissions WHERE trainer_email = ?`
    ).bind(trainerEmail).first();
    if (!row) return true;
    return !!row.val;
  } catch (e) {
    return true;
  }
}
__name(hasTrainerPermission, "hasTrainerPermission");
__name2(hasTrainerPermission, "hasTrainerPermission");
var CONTACT_TABLES_READY = false;
async function ensureContactTables(env) {
  if (CONTACT_TABLES_READY) return;
  await env.DB.batch([
    env.DB.prepare(`CREATE TABLE IF NOT EXISTS contact_lists (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, owner_email TEXT NOT NULL, created_at INTEGER, updated_at INTEGER)`),
    env.DB.prepare(`CREATE TABLE IF NOT EXISTS contact_list_members (id INTEGER PRIMARY KEY AUTOINCREMENT, list_id INTEGER NOT NULL, ns_student_id TEXT NOT NULL, name TEXT NOT NULL, phone TEXT NOT NULL, note TEXT, added_by TEXT, created_at INTEGER, UNIQUE(list_id, ns_student_id))`),
    env.DB.prepare(`CREATE TABLE IF NOT EXISTS student_phone_consent (ns_student_id TEXT PRIMARY KEY, name TEXT, phone TEXT, consent INTEGER NOT NULL DEFAULT 1, confirmed_by TEXT, confirmed_at INTEGER)`),
    env.DB.prepare(`CREATE TABLE IF NOT EXISTS harmonogram_contact_lists (visit_id TEXT PRIMARY KEY, list_id INTEGER NOT NULL, ns_student_id TEXT NOT NULL, attached_by TEXT, updated_at INTEGER)`)
  ]);
  try {
    await env.DB.prepare("ALTER TABLE contact_list_members ADD COLUMN note TEXT").run();
  } catch (e) {
  }
  CONTACT_TABLES_READY = true;
}
__name(ensureContactTables, "ensureContactTables");
async function ensureAckTable(env) {
  await env.DB.prepare(`CREATE TABLE IF NOT EXISTS student_acks (id INTEGER PRIMARY KEY AUTOINCREMENT, ns_student_id TEXT NOT NULL, student_email TEXT, kind TEXT NOT NULL, ref TEXT NOT NULL, ack TEXT NOT NULL, text TEXT, created_at INTEGER, seen_by_trainer INTEGER DEFAULT 0)`).run();
}
__name(ensureAckTable, "ensureAckTable");
__name2(ensureAckTable, "ensureAckTable");
async function handleStudentAckSave(request, env) {
  let b = {};
  try {
    b = await request.json();
  } catch (e) {
  }
  const valid = await validateNsToken(b.ns_token, b.ns_email, b.ns_server);
  if (!valid) return json({ error: "Nieprawid\u0142owy lub wygas\u0142y token NS \u2014 zaloguj si\u0119 ponownie" }, 401);
  const kind = b.kind === "diary" ? "diary" : b.kind === "record" ? "record" : null;
  const ack = ["ok", "question", "listened"].includes(b.ack) ? b.ack : null;
  const ref = String(b.ref || "").slice(0, 64);
  if (!kind || !ack || !ref) return json({ error: "Brak danych" }, 400);
  const text = String(b.text || "").trim().slice(0, 2e3) || null;
  if (ack === "question" && !text) return json({ error: "Wpisz pytanie" }, 400);
  const u = await nsUserByEmail(env, b.ns_email);
  if (!u) return json({ error: "Nie znaleziono kursanta" }, 404);
  await ensureAckTable(env);
  const sid = String(u.id), now = Math.floor(Date.now() / 1e3);
  if (ack !== "question") {
    const ex = await env.DB.prepare("SELECT id FROM student_acks WHERE ns_student_id = ? AND kind = ? AND ref = ? AND ack = ?").bind(sid, kind, ref, ack).first();
    if (ex) {
      await env.DB.prepare("UPDATE student_acks SET created_at = ? WHERE id = ?").bind(now, ex.id).run();
      return json({ ok: true, id: ex.id });
    }
  }
  const r = await env.DB.prepare("INSERT INTO student_acks (ns_student_id, student_email, kind, ref, ack, text, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)").bind(sid, String(b.ns_email).toLowerCase(), kind, ref, ack, text, now).run();
  return json({ ok: true, id: r.meta.last_row_id });
}
__name(handleStudentAckSave, "handleStudentAckSave");
__name2(handleStudentAckSave, "handleStudentAckSave");
async function handleStudentAckMine(request, env, url) {
  const t = url.searchParams.get("ns_token"), e = url.searchParams.get("ns_email"), sv = url.searchParams.get("ns_server");
  const valid = await validateNsToken(t, e, sv);
  if (!valid) return json({ error: "Nieprawid\u0142owy lub wygas\u0142y token NS \u2014 zaloguj si\u0119 ponownie" }, 401);
  const u = await nsUserByEmail(env, e);
  if (!u) return json({ ok: true, acks: [] });
  await ensureAckTable(env);
  const rows = (await env.DB.prepare("SELECT kind, ref, ack, text, created_at FROM student_acks WHERE ns_student_id = ? ORDER BY created_at DESC LIMIT 300").bind(String(u.id)).all()).results || [];
  return json({ ok: true, acks: rows });
}
__name(handleStudentAckMine, "handleStudentAckMine");
__name2(handleStudentAckMine, "handleStudentAckMine");
async function handleStudentAcksList(request, env, url) {
  const who = await contactAuth(env, url.searchParams.get("ns_token"), url.searchParams.get("ns_email"), url.searchParams.get("ns_server"));
  if (!who) return json({ error: "Nieprawid\u0142owy lub wygas\u0142y token NS \u2014 zaloguj si\u0119 ponownie" }, 401);
  if (who.forbidden) return json({ error: "Tylko dla trenera" }, 403);
  await ensureAckTable(env);
  const sid = url.searchParams.get("ns_student_id"), since = parseInt(url.searchParams.get("since") || "0", 10) || 0;
  const unseen = url.searchParams.get("only_unseen") === "1";
  let q = "SELECT * FROM student_acks WHERE created_at >= ?", args = [since];
  if (sid) {
    q += " AND ns_student_id = ?";
    args.push(String(sid));
  }
  if (unseen) q += " AND seen_by_trainer = 0";
  q += " ORDER BY created_at DESC LIMIT 500";
  const rows = (await env.DB.prepare(q).bind(...args).all()).results || [];
  return json({ ok: true, acks: rows });
}
__name(handleStudentAcksList, "handleStudentAcksList");
__name2(handleStudentAcksList, "handleStudentAcksList");
async function handleStudentAcksSeen(request, env) {
  let b = {};
  try {
    b = await request.json();
  } catch (e) {
  }
  const who = await contactAuth(env, b.ns_token, b.ns_email, b.ns_server);
  if (!who) return json({ error: "Nieprawid\u0142owy lub wygas\u0142y token NS \u2014 zaloguj si\u0119 ponownie" }, 401);
  if (who.forbidden) return json({ error: "Tylko dla trenera" }, 403);
  await ensureAckTable(env);
  const ids = (Array.isArray(b.ids) ? b.ids : []).map((x) => parseInt(x, 10)).filter((x) => x > 0).slice(0, 500);
  for (const id of ids) await env.DB.prepare("UPDATE student_acks SET seen_by_trainer = 1 WHERE id = ?").bind(id).run();
  return json({ ok: true, updated: ids.length });
}
__name(handleStudentAcksSeen, "handleStudentAcksSeen");
__name2(handleStudentAcksSeen, "handleStudentAcksSeen");
async function handleNsRecordAudio(request, env, url) {
  const id = url.searchParams.get("record_id"), t = url.searchParams.get("ns_token"), e = url.searchParams.get("ns_email"), sv = url.searchParams.get("ns_server");
  if (!id || !/^[0-9a-zA-Z-]+$/.test(id) || !t || !e) return new Response("Brak danych", { status: 400, headers: CORS });
  const base = nsServerBase(sv);
  const H = { "Accept": "application/json", "Authorization": `Token token=${t}`, "Email": e, "X-Requested-With": "XMLHttpRequest" };
  const r = await fetch(`${base}/api/v1/records/${id}`, { headers: H });
  if (!r.ok) return new Response("Brak dost\u0119pu do nagrania", { status: r.status === 404 ? 404 : 403, headers: CORS });
  const d = await r.json().catch(() => null);
  const rec = d && d.record ? d.record : d;
  let f = rec && rec.record_file_url;
  if (!f) return new Response("Nagranie nie ma pliku", { status: 404, headers: CORS });
  if (f.startsWith("/public/")) f = f.slice("/public".length);
  const full = /^https?:/.test(f) ? f : base + f;
  const a = await fetch(full, { headers: H });
  if (!a.ok) return new Response("Nie uda\u0142o si\u0119 pobra\u0107 pliku", { status: 502, headers: CORS });
  const h = new Headers(CORS);
  h.set("Content-Type", a.headers.get("Content-Type") || "audio/mpeg");
  h.set("Cache-Control", "private, max-age=3600");
  return new Response(a.body, { headers: h });
}
__name(handleNsRecordAudio, "handleNsRecordAudio");
__name2(handleNsRecordAudio, "handleNsRecordAudio");
var DISC_READY = false;
async function ensureDiscTable(env) {
  if (DISC_READY) return;
  await env.DB.prepare(`CREATE TABLE IF NOT EXISTS disc_results (id INTEGER PRIMARY KEY AUTOINCREMENT, ns_user_id TEXT NOT NULL, answers_most TEXT NOT NULL, answers_least TEXT NOT NULL, scores TEXT, profile_code TEXT, profile_title TEXT, created_at INTEGER)`).run();
  await env.DB.prepare(`CREATE INDEX IF NOT EXISTS idx_disc_user ON disc_results (ns_user_id, created_at)`).run();
  DISC_READY = true;
}
__name(ensureDiscTable, "ensureDiscTable");
__name2(ensureDiscTable, "ensureDiscTable");
function discAnswersOk(a) {
  return Array.isArray(a) && a.length === 24 && a.every((x) => Number.isInteger(x) && x >= 0 && x <= 3);
}
__name(discAnswersOk, "discAnswersOk");
__name2(discAnswersOk, "discAnswersOk");
async function discTrainerCanSee(env, who, studentId) {
  if (who.admin) return true;
  const pg = new Client({ connectionString: env.HYPERDRIVE.connectionString });
  try {
    await pg.connect();
    const acc = await pg.query(
      `SELECT 1 FROM users s
         JOIN students_groups_users sgu ON sgu.students_group_id = s.students_group_id
         JOIN users t ON t.id = sgu.user_id
       WHERE s.id = $1 AND LOWER(t.email) = LOWER($2) LIMIT 1`,
      [studentId, who.email]
    );
    return acc.rows.length > 0;
  } catch (e) {
    return false;
  } finally {
    try {
      await pg.end();
    } catch (e) {
    }
  }
}
__name(discTrainerCanSee, "discTrainerCanSee");
__name2(discTrainerCanSee, "discTrainerCanSee");
async function handleDiscSave(request, env) {
  let body = {};
  try {
    body = await request.json();
  } catch (e) {
  }
  const valid = await validateNsToken(body.ns_token, body.ns_email, body.ns_server);
  if (!valid) return json({ error: "Nieprawid\u0142owy lub wygas\u0142y token NS \u2014 zaloguj si\u0119 ponownie" }, 401);
  const u = await nsUserByEmail(env, body.ns_email);
  if (!u) return json({ error: "Nie znaleziono konta" }, 404);
  if (!discAnswersOk(body.most) || !discAnswersOk(body.least)) return json({ error: "Niepe\u0142ne odpowiedzi (potrzeba 24 + 24)" }, 400);
  await ensureDiscTable(env);
  const sid = String(u.id), now = Math.floor(Date.now() / 1e3);
  const cnt = await env.DB.prepare("SELECT COUNT(*) AS n FROM disc_results WHERE ns_user_id = ? AND created_at > ?").bind(sid, now - 86400).first();
  if (cnt && cnt.n >= 10) return json({ error: "Za du\u017Co test\xF3w dzisiaj \u2014 spr\xF3buj jutro" }, 429);
  const scores = body.scores ? JSON.stringify(body.scores).slice(0, 4e3) : null;
  const r = await env.DB.prepare(`INSERT INTO disc_results (ns_user_id, answers_most, answers_least, scores, profile_code, profile_title, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)`).bind(sid, JSON.stringify(body.most), JSON.stringify(body.least), scores, String(body.profile_code || "").slice(0, 20), String(body.profile_title || "").slice(0, 80), now).run();
  return json({ ok: true, id: r.meta && r.meta.last_row_id, created_at: now });
}
__name(handleDiscSave, "handleDiscSave");
__name2(handleDiscSave, "handleDiscSave");
async function handleDiscList(request, env, url) {
  const t = url.searchParams.get("ns_token"), e = url.searchParams.get("ns_email"), sv = url.searchParams.get("ns_server");
  const studentId = url.searchParams.get("student_id");
  let sid;
  if (studentId) {
    const who = await contactAuth(env, t, e, sv);
    if (!who) return json({ error: "Nieprawid\u0142owy lub wygas\u0142y token NS \u2014 zaloguj si\u0119 ponownie" }, 401);
    if (who.forbidden) return json({ error: "Tylko trener/admin" }, 403);
    if (!await discTrainerCanSee(env, who, studentId)) return json({ error: "Ten kursant nie nale\u017Cy do Twoich grup" }, 403);
    sid = String(studentId);
  } else {
    const valid = await validateNsToken(t, e, sv);
    if (!valid) return json({ error: "Nieprawid\u0142owy lub wygas\u0142y token NS \u2014 zaloguj si\u0119 ponownie" }, 401);
    const u = await nsUserByEmail(env, e);
    if (!u) return json({ ok: true, results: [] });
    sid = String(u.id);
  }
  await ensureDiscTable(env);
  const rows = (await env.DB.prepare("SELECT id, answers_most, answers_least, profile_code, profile_title, created_at FROM disc_results WHERE ns_user_id = ? ORDER BY created_at DESC LIMIT 50").bind(sid).all()).results || [];
  return json({ ok: true, results: rows.map((r) => ({ ...r, answers_most: JSON.parse(r.answers_most), answers_least: JSON.parse(r.answers_least) })) });
}
__name(handleDiscList, "handleDiscList");
__name2(handleDiscList, "handleDiscList");
async function handleDiscOverview(request, env, url) {
  const who = await contactAuth(env, url.searchParams.get("ns_token"), url.searchParams.get("ns_email"), url.searchParams.get("ns_server"));
  if (!who) return json({ error: "Nieprawid\u0142owy lub wygas\u0142y token NS \u2014 zaloguj si\u0119 ponownie" }, 401);
  if (who.forbidden) return json({ error: "Tylko trener/admin" }, 403);
  await ensureDiscTable(env);
  const latest = (await env.DB.prepare(`SELECT d.ns_user_id, d.id, d.profile_code, d.profile_title, d.created_at,
      (SELECT COUNT(*) FROM disc_results x WHERE x.ns_user_id = d.ns_user_id) AS tests
    FROM disc_results d WHERE d.id = (SELECT id FROM disc_results y WHERE y.ns_user_id = d.ns_user_id ORDER BY created_at DESC, id DESC LIMIT 1)`).all()).results || [];
  const byId = {};
  latest.forEach((r) => {
    byId[String(r.ns_user_id)] = r;
  });
  const pg = new Client({ connectionString: env.HYPERDRIVE.connectionString });
  let students = [];
  try {
    await pg.connect();
    if (who.admin) {
      const ids = Object.keys(byId);
      if (ids.length) students = (await pg.query(`SELECT id, first_name, last_name, email FROM users WHERE id::text = ANY($1::text[]) AND deleted_at IS NULL`, [ids])).rows;
    } else {
      students = (await pg.query(
        `SELECT DISTINCT s.id, s.first_name, s.last_name, s.email FROM users s
           JOIN students_groups_users sgu ON sgu.students_group_id = s.students_group_id
           JOIN users t ON t.id = sgu.user_id
         WHERE LOWER(t.email) = LOWER($1) AND s.deleted_at IS NULL`,
        [who.email]
      )).rows;
    }
  } catch (e) {
    return json({ error: "Baza NS niedost\u0119pna: " + e.message }, 502);
  } finally {
    try {
      await pg.end();
    } catch (e) {
    }
  }
  const out = students.map((s) => {
    const r = byId[String(s.id)] || null;
    return {
      student_id: String(s.id),
      name: `${s.first_name || ""} ${s.last_name || ""}`.trim() || s.email,
      email: s.email,
      profile_code: r ? r.profile_code : null,
      profile_title: r ? r.profile_title : null,
      created_at: r ? r.created_at : null,
      tests: r ? r.tests : 0,
      result_id: r ? r.id : null
    };
  });
  out.sort((a, b) => (b.created_at || 0) - (a.created_at || 0) || a.name.localeCompare(b.name, "pl"));
  return json({ ok: true, students: out });
}
__name(handleDiscOverview, "handleDiscOverview");
__name2(handleDiscOverview, "handleDiscOverview");
async function handleDiscDelete(request, env) {
  let body = {};
  try {
    body = await request.json();
  } catch (e) {
  }
  const who = await contactAuth(env, body.ns_token, body.ns_email, body.ns_server);
  if (!who) return json({ error: "Nieprawid\u0142owy lub wygas\u0142y token NS \u2014 zaloguj si\u0119 ponownie" }, 401);
  if (who.forbidden) return json({ error: "Tylko trener/admin" }, 403);
  await ensureDiscTable(env);
  const row = await env.DB.prepare("SELECT ns_user_id FROM disc_results WHERE id = ?").bind(Number(body.id) || 0).first();
  if (!row) return json({ error: "Nie ma takiego wyniku" }, 404);
  if (!await discTrainerCanSee(env, who, row.ns_user_id)) return json({ error: "Ten kursant nie nale\u017Cy do Twoich grup" }, 403);
  await env.DB.prepare("DELETE FROM disc_results WHERE id = ?").bind(Number(body.id)).run();
  return json({ ok: true });
}
__name(handleDiscDelete, "handleDiscDelete");
__name2(handleDiscDelete, "handleDiscDelete");
async function contactAuth(env, nsToken, nsEmail, nsServer) {
  const valid = await validateNsToken(nsToken, nsEmail, nsServer);
  if (!valid) return null;
  const u = await nsUserByEmail(env, nsEmail);
  const role = String(u && u.role || "").toLowerCase();
  if (!u || /student|client|kursant/.test(role) || role === "") return { forbidden: true };
  return { email: String(nsEmail).toLowerCase(), admin: role === "admin" };
}
__name(contactAuth, "contactAuth");
var NS_USER_MEM = /* @__PURE__ */ new Map();
async function nsUserByEmail(env, email) {
  if (!email) return null;
  const em = String(email).toLowerCase(), now = Math.floor(Date.now() / 1e3);
  const mm = NS_USER_MEM.get(em);
  if (mm && mm.until > now) return mm.u;
  try {
    await speedTables(env);
    const row = await env.DB.prepare("SELECT ns_id, role, at FROM ns_user_cache WHERE email = ?").bind(em).first();
    if (row && row.at > now - 7 * 86400) {
      const u = { id: row.ns_id, role: row.role };
      NS_USER_MEM.set(em, { u, until: now + 3600 });
      return u;
    }
  } catch (e) {
  }
  const pgClient = new Client({ connectionString: env.HYPERDRIVE.connectionString });
  try {
    await pgClient.connect();
    const r = await pgClient.query(`SELECT id, role FROM users WHERE LOWER(email) = LOWER($1) AND deleted_at IS NULL LIMIT 1`, [email]);
    const u = r.rows[0] || null;
    if (u) {
      NS_USER_MEM.set(em, { u, until: now + 3600 });
      speedLater(env.DB.prepare("INSERT INTO ns_user_cache (email, ns_id, role, at) VALUES (?, ?, ?, ?) ON CONFLICT(email) DO UPDATE SET ns_id = excluded.ns_id, role = excluded.role, at = excluded.at").bind(em, String(u.id), u.role || "", now).run());
    }
    return u;
  } catch (e) {
    return null;
  } finally {
    try {
      await pgClient.end();
    } catch (e) {
    }
  }
}
__name(nsUserByEmail, "nsUserByEmail");
__name2(nsUserByEmail, "nsUserByEmail");
var CALL_DAYS_TABLE_READY = false;
async function ensureCallDaysTable(env) {
  if (CALL_DAYS_TABLE_READY) return;
  await env.DB.prepare(`CREATE TABLE IF NOT EXISTS call_days (id INTEGER PRIMARY KEY AUTOINCREMENT, ns_student_id TEXT NOT NULL, student_name TEXT, day TEXT NOT NULL, time TEXT, note TEXT, trainer_email TEXT, trainer_name TEXT, trainer_phone TEXT, gcal_event_id TEXT, created_at INTEGER)`).run();
  try {
    await env.DB.prepare("ALTER TABLE call_days ADD COLUMN called INTEGER").run();
  } catch (e) {
  }
  try {
    await env.DB.prepare("ALTER TABLE call_days ADD COLUMN called_at INTEGER").run();
  } catch (e) {
  }
  CALL_DAYS_TABLE_READY = true;
}
__name(ensureCallDaysTable, "ensureCallDaysTable");
__name2(ensureCallDaysTable, "ensureCallDaysTable");
function b64url(buf) {
  const bytes = typeof buf === "string" ? new TextEncoder().encode(buf) : new Uint8Array(buf);
  let s = "";
  for (let i = 0; i < bytes.length; i++) s += String.fromCharCode(bytes[i]);
  return btoa(s).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}
__name(b64url, "b64url");
__name2(b64url, "b64url");
function warsawIso(d) {
  if (!d) return d;
  const dt = d instanceof Date ? d : new Date(d);
  if (isNaN(dt)) return String(d);
  const local = new Intl.DateTimeFormat("sv-SE", { timeZone: "Europe/Warsaw", year: "numeric", month: "2-digit", day: "2-digit", hour: "2-digit", minute: "2-digit", second: "2-digit", hour12: false }).format(dt).replace(" ", "T");
  const asUtc = Date.parse(local + "Z");
  const offMin = Math.round((asUtc - dt.getTime()) / 6e4);
  const sign = offMin >= 0 ? "+" : "-", a = Math.abs(offMin);
  return local + sign + String(Math.floor(a / 60)).padStart(2, "0") + ":" + String(a % 60).padStart(2, "0");
}
__name(warsawIso, "warsawIso");
async function handleTrainerVisitDates(request, env, url) {
  const who = await contactAuth(env, url.searchParams.get("ns_token"), url.searchParams.get("ns_email"), url.searchParams.get("ns_server"));
  if (!who) return json({ error: "Nieprawid\u0142owy lub wygas\u0142y token NS \u2014 zaloguj si\u0119 ponownie" }, 401);
  if (who.forbidden) return json({ error: "Tylko trener/admin" }, 403);
  const studentId = url.searchParams.get("student_id");
  if (!studentId) return json({ error: "Brak student_id" }, 400);
  const pg = new Client({ connectionString: env.HYPERDRIVE.connectionString });
  try {
    await pg.connect();
    if (!who.admin) {
      const acc = await pg.query(
        `SELECT 1 FROM users s
           JOIN students_groups_users sgu ON sgu.students_group_id = s.students_group_id
           JOIN users t ON t.id = sgu.user_id
         WHERE s.id = $1 AND LOWER(t.email) = LOWER($2) LIMIT 1`,
        [studentId, who.email]
      );
      if (!acc.rows.length) return json({ error: "Ten kursant nie nale\u017Cy do Twoich grup" }, 403);
    }
    const nocache = ` /* fresh ${Date.now()}-${Math.random().toString(36).slice(2)} */`;
    const v = await pg.query(
      `SELECT id, date, obligatory, required_diaries_count, additional_requirements, student_id, trainer_id, created_at, now() AS _fresh
         FROM visit_dates WHERE student_id = $1 ORDER BY date ASC` + nocache,
      [studentId]
    );
    const ids = v.rows.map((r) => r.id);
    let reqs = [];
    if (ids.length) {
      reqs = (await pg.query(
        `SELECT r.id, r.visit_date_id, r.record_category_id, r.records_count, MAX(now()) AS _fresh,
                MAX(CASE WHEN t.locale = 'pl' THEN t.name END) AS name_pl,
                MAX(CASE WHEN t.locale = 'cz' THEN t.name END) AS name_cz,
                MAX(CASE WHEN t.locale = 'en' THEN t.name END) AS name_en
           FROM visit_record_requirements r
           LEFT JOIN record_category_translations t ON t.record_category_id = r.record_category_id
          WHERE r.visit_date_id = ANY($1::uuid[])
          GROUP BY r.id, r.visit_date_id, r.record_category_id, r.records_count` + nocache,
        [ids]
      )).rows;
    }
    const visits = v.rows.map((row) => ({
      id: row.id,
      date: warsawIso(row.date),
      obligatory: row.obligatory,
      required_diaries_count: row.required_diaries_count,
      additional_requirements: row.additional_requirements,
      student_id: row.student_id,
      trainer_id: row.trainer_id,
      visit_record_requirements: reqs.filter((r) => r.visit_date_id === row.id).map((r) => ({
        id: r.id,
        record_category_id: r.record_category_id,
        records_count: r.records_count,
        record_category: { id: r.record_category_id, name_pl: r.name_pl, name_cz: r.name_cz, name_en: r.name_en, name: r.name_pl }
      }))
    }));
    let stats = null;
    if (url.searchParams.get("stats") && v.rows.length) {
      const warsawDay = /* @__PURE__ */ __name((d) => d ? new Intl.DateTimeFormat("sv-SE", { timeZone: "Europe/Warsaw", year: "numeric", month: "2-digit", day: "2-digit" }).format(d instanceof Date ? d : new Date(d)) : null, "warsawDay");
      const prevDay = /* @__PURE__ */ __name((ymd) => {
        const d = /* @__PURE__ */ new Date(ymd + "T12:00:00Z");
        d.setUTCDate(d.getUTCDate() - 1);
        return d.toISOString().slice(0, 10);
      }, "prevDay");
      const stu = (await pg.query(`SELECT created_at FROM users WHERE id = $1` + nocache, [studentId])).rows[0];
      const recs = (await pg.query(
        `SELECT r.record_category_id AS cat, r.date, r.is_correct, now() AS _fresh
           FROM records r WHERE r.user_id = $1 AND r.deleted_at IS NULL` + nocache,
        [studentId]
      )).rows;
      const catNames = {};
      const catIds = [...new Set(recs.map((r) => r.cat))];
      if (catIds.length) {
        (await pg.query(
          `SELECT record_category_id AS id, MAX(CASE WHEN locale='pl' THEN name END) AS pl FROM record_category_translations
            WHERE record_category_id = ANY($1::uuid[]) GROUP BY record_category_id` + nocache,
          [catIds]
        )).rows.forEach((c) => {
          catNames[c.id] = c.pl;
        });
      }
      const recDays = recs.map((r) => ({ cat: r.cat, day: warsawDay(r.date), ok: r.is_correct === true }));
      let diaryDays = [];
      try {
        diaryDays = (await pg.query(`SELECT date, is_correct, now() AS _fresh FROM diaries WHERE user_id = $1 AND deleted_at IS NULL` + nocache, [studentId])).rows.map((d) => ({ day: warsawDay(d.date), ok: d.is_correct === true }));
      } catch (e) {
      }
      let from = stu && stu.created_at ? warsawDay(stu.created_at) : "1970-01-01";
      stats = v.rows.map((row) => {
        const visitDay = warsawDay(row.date);
        const to = prevDay(visitDay);
        const per = {};
        reqs.filter((r) => r.visit_date_id === row.id).forEach((r) => {
          per[r.record_category_id] = { category_id: r.record_category_id, category_name: r.name_pl || catNames[r.record_category_id] || "", required_records: Number(r.records_count) || 0, all_records: 0, correct_records: 0 };
        });
        recDays.forEach((r) => {
          if (!r.day || r.day < from || r.day > to) return;
          const c = per[r.cat] || (per[r.cat] = { category_id: r.cat, category_name: catNames[r.cat] || "", required_records: 0, all_records: 0, correct_records: 0 });
          c.all_records++;
          if (r.ok) c.correct_records++;
        });
        const cats = Object.values(per);
        const dia = diaryDays.filter((d) => d.day && d.day >= from && d.day <= to);
        const out = {
          visit_id: row.id,
          start_range: from,
          end_range: to,
          recorded_records_categories: cats,
          all_records: cats.reduce((a, c) => a + c.all_records, 0),
          correct_records: cats.reduce((a, c) => a + c.correct_records, 0),
          required_records: cats.reduce((a, c) => a + c.required_records, 0),
          all_diaries: dia.length,
          correct_diaries: dia.filter((d) => d.ok).length,
          required_diaries: Number(row.required_diaries_count) || 0
        };
        from = visitDay;
        return out;
      });
    }
    return json({ ok: true, collection: visits, stats });
  } catch (e) {
    return json({ error: "B\u0142\u0105d bazy NS: " + e.message }, 500);
  } finally {
    try {
      await pg.end();
    } catch (e) {
    }
  }
}
__name(handleTrainerVisitDates, "handleTrainerVisitDates");
__name2(handleTrainerVisitDates, "handleTrainerVisitDates");
var DOC_ISSUERS_DEFAULT = {
  // nr konta i KRS ze stron nowamowa.com (kontakt / o nas i fundacja)
  ns: { prefix: "NS", name: "NEW SPEECH Sp. z o.o.", addr1: "ul. Rynek 3/2", addr2: "43-190 Miko\u0142\xF3w", nip: "635-183-97-54", regon: "", krs: "0000642335", bank: "", account: "77 1090 2590 0000 0001 3349 8838", vat_note: "Zwolnione z VAT na podstawie art. 43 ust. 1" },
  // dane Fundacji z metryki KRS (podane przez Dawida)
  fnm: { prefix: "FNM", name: "Fundacja Nowa Mowa", addr1: "Rynek 3 lok. 2", addr2: "43-190 Miko\u0142\xF3w", nip: "635-187-98-02", regon: "544354863", krs: "0001231820", bank: "BNP Paribas S.A.", account: "22 1600 1462 1729 6672 3000 0001", vat_note: "Zwolnione z VAT na podstawie art. 43 ust. 1" }
};
var DOC_TABLES_READY = false;
async function ensureDocTables(env) {
  if (DOC_TABLES_READY) return;
  await ensureGoogleTables(env);
  await env.DB.batch([
    env.DB.prepare("CREATE TABLE IF NOT EXISTS issued_documents (id INTEGER PRIMARY KEY AUTOINCREMENT, ns_student_id TEXT NOT NULL, student_name TEXT, doc_type TEXT NOT NULL, issuer TEXT, number TEXT, title TEXT, payload TEXT, created_by TEXT, created_at INTEGER)"),
    env.DB.prepare("CREATE TABLE IF NOT EXISTS doc_counters (issuer TEXT NOT NULL, year INTEGER NOT NULL, last INTEGER NOT NULL DEFAULT 0, PRIMARY KEY (issuer, year))")
  ]);
  DOC_TABLES_READY = true;
}
__name(ensureDocTables, "ensureDocTables");
async function docIssuers(env) {
  const raw = await appSettingGet(env, "doc_issuers");
  let saved = {};
  try {
    saved = raw ? JSON.parse(raw) : {};
  } catch (e) {
  }
  const out = {};
  for (const k of Object.keys(DOC_ISSUERS_DEFAULT)) {
    out[k] = Object.assign({}, DOC_ISSUERS_DEFAULT[k]);
    const sv = saved[k] || {};
    for (const f of Object.keys(sv)) if (String(sv[f] || "").trim() !== "") out[k][f] = sv[f];
  }
  return out;
}
__name(docIssuers, "docIssuers");
async function handleDocumentsList(request, env, url) {
  const who = await contactAuth(env, url.searchParams.get("ns_token"), url.searchParams.get("ns_email"), url.searchParams.get("ns_server"));
  if (!who) return json({ error: "Nieprawid\u0142owy lub wygas\u0142y token NS \u2014 zaloguj si\u0119 ponownie" }, 401);
  if (who.forbidden) return json({ error: "Dokumenty wystawia trener" }, 403);
  await ensureDocTables(env);
  const sid = url.searchParams.get("ns_student_id");
  const rows = sid ? (await env.DB.prepare("SELECT * FROM issued_documents WHERE ns_student_id = ? ORDER BY created_at DESC LIMIT 200").bind(String(sid)).all()).results || [] : (await env.DB.prepare("SELECT id, ns_student_id, student_name, doc_type, issuer, number, title, created_by, created_at FROM issued_documents ORDER BY created_at DESC LIMIT 300").all()).results || [];
  return json({ ok: true, is_admin: who.admin, documents: rows, issuers: await docIssuers(env) });
}
__name(handleDocumentsList, "handleDocumentsList");
__name2(handleDocumentsList, "handleDocumentsList");
async function handleDocumentsAction(request, env, action) {
  let body = {};
  try {
    body = await request.json();
  } catch (e) {
  }
  const who = await contactAuth(env, body.ns_token, body.ns_email, body.ns_server);
  if (!who) return json({ error: "Nieprawid\u0142owy lub wygas\u0142y token NS \u2014 zaloguj si\u0119 ponownie" }, 401);
  if (who.forbidden) return json({ error: "Dokumenty wystawia trener" }, 403);
  await ensureDocTables(env);
  const now = Math.floor(Date.now() / 1e3);
  if (action === "save") {
    if (!body.ns_student_id || !body.doc_type) return json({ error: "Brak kursanta lub rodzaju dokumentu" }, 400);
    const payload = body.payload && typeof body.payload === "object" ? body.payload : {};
    let number = null, issuer = null;
    if (body.doc_type === "invoice") {
      issuer = body.issuer === "fnm" ? "fnm" : "ns";
      const issuers = await docIssuers(env);
      const manual = String(body.number || "").trim();
      if (manual) {
        const dup = await env.DB.prepare("SELECT id FROM issued_documents WHERE doc_type = 'invoice' AND issuer = ? AND number = ?").bind(issuer, manual).first();
        if (dup) return json({ error: "Rachunek o numerze " + manual + " ju\u017C istnieje" }, 409);
        number = manual;
      } else {
        const year = parseInt(String(payload.date || "").slice(0, 4), 10) || (/* @__PURE__ */ new Date()).getFullYear();
        await env.DB.prepare("INSERT INTO doc_counters (issuer, year, last) VALUES (?, ?, 0) ON CONFLICT(issuer, year) DO NOTHING").bind(issuer, year).run();
        const r = await env.DB.prepare("UPDATE doc_counters SET last = last + 1 WHERE issuer = ? AND year = ? RETURNING last").bind(issuer, year).first();
        number = `${issuers[issuer].prefix}/${r.last}/${year}`;
      }
      payload.number = number;
      payload.issuer_data = issuers[issuer];
    }
    const r2 = await env.DB.prepare("INSERT INTO issued_documents (ns_student_id, student_name, doc_type, issuer, number, title, payload, created_by, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)").bind(String(body.ns_student_id), String(body.student_name || ""), String(body.doc_type), issuer, number, String(body.title || "").slice(0, 300), JSON.stringify(payload), who.email, now).run();
    return json({ ok: true, id: r2.meta.last_row_id, number, payload });
  }
  if (action === "update") {
    const row = await env.DB.prepare("SELECT * FROM issued_documents WHERE id = ?").bind(body.id).first();
    if (!row) return json({ error: "Nie ma takiego dokumentu" }, 404);
    if (!who.admin && String(row.created_by).toLowerCase() !== who.email) return json({ error: "Ten dokument wystawi\u0142 inny trener" }, 403);
    const payload = body.payload && typeof body.payload === "object" ? body.payload : {};
    let number = row.number;
    if (row.doc_type === "invoice") {
      const manual = String(body.number || "").trim();
      if (manual && manual !== row.number) {
        const dup = await env.DB.prepare("SELECT id FROM issued_documents WHERE doc_type = 'invoice' AND issuer = ? AND number = ? AND id <> ?").bind(row.issuer, manual, row.id).first();
        if (dup) return json({ error: "Rachunek o numerze " + manual + " ju\u017C istnieje" }, 409);
        number = manual;
      }
      payload.number = number;
      payload.issuer = row.issuer;
      payload.issuer_data = (await docIssuers(env))[row.issuer];
    }
    await env.DB.prepare("UPDATE issued_documents SET payload = ?, title = ?, number = ? WHERE id = ?").bind(JSON.stringify(payload), String(body.title || row.title || "").slice(0, 300), number, row.id).run();
    return json({ ok: true, id: row.id, number, payload });
  }
  if (action === "delete") {
    const row = await env.DB.prepare("SELECT created_by, doc_type FROM issued_documents WHERE id = ?").bind(body.id).first();
    if (!row) return json({ ok: true });
    if (!who.admin && String(row.created_by).toLowerCase() !== who.email) return json({ error: "Ten dokument wystawi\u0142 inny trener" }, 403);
    await env.DB.prepare("DELETE FROM issued_documents WHERE id = ?").bind(body.id).run();
    return json({ ok: true });
  }
  if (action === "issuers") {
    if (!who.admin) return json({ error: "Dane wystawc\xF3w zmienia tylko admin" }, 403);
    const cur = await docIssuers(env);
    const inc = body.issuers || {};
    const fields = ["prefix", "name", "addr1", "addr2", "nip", "regon", "krs", "bank", "account", "vat_note"];
    for (const k of Object.keys(cur)) {
      if (!inc[k]) continue;
      for (const f of fields) if (inc[k][f] !== void 0) cur[k][f] = String(inc[k][f]).trim().slice(0, 200);
      if (!cur[k].prefix) cur[k].prefix = DOC_ISSUERS_DEFAULT[k].prefix;
    }
    await appSettingSet(env, "doc_issuers", JSON.stringify(cur));
    return json({ ok: true, issuers: cur });
  }
  return json({ error: "Nieznana akcja" }, 404);
}
__name(handleDocumentsAction, "handleDocumentsAction");
__name2(handleDocumentsAction, "handleDocumentsAction");
var GOOGLE_OAUTH_SCOPE = "https://www.googleapis.com/auth/calendar https://www.googleapis.com/auth/gmail.send";
function googleOAuthRedirectUri(url) {
  return url.origin + "/google/oauth/callback";
}
__name(googleOAuthRedirectUri, "googleOAuthRedirectUri");
async function ensureGoogleTables(env) {
  await env.DB.batch([
    env.DB.prepare("CREATE TABLE IF NOT EXISTS app_settings (key TEXT PRIMARY KEY, value TEXT, updated_at INTEGER)"),
    env.DB.prepare("CREATE TABLE IF NOT EXISTS oauth_states (state TEXT PRIMARY KEY, email TEXT, created_at INTEGER)")
  ]);
}
__name(ensureGoogleTables, "ensureGoogleTables");
async function appSettingGet(env, key) {
  try {
    const r = await env.DB.prepare("SELECT value FROM app_settings WHERE key = ?").bind(key).first();
    return r ? r.value : null;
  } catch (e) {
    return null;
  }
}
__name(appSettingGet, "appSettingGet");
async function appSettingSet(env, key, value) {
  await env.DB.prepare("INSERT INTO app_settings (key, value, updated_at) VALUES (?, ?, ?) ON CONFLICT(key) DO UPDATE SET value = excluded.value, updated_at = excluded.updated_at").bind(key, value, Math.floor(Date.now() / 1e3)).run();
}
__name(appSettingSet, "appSettingSet");
var GOOGLE_OAUTH_CACHE = { token: null, exp: 0 };
async function googleOAuthAccessToken(env) {
  if (!env.GOOGLE_OAUTH_CLIENT_ID || !env.GOOGLE_OAUTH_CLIENT_SECRET) return null;
  if (GOOGLE_OAUTH_CACHE.token && Date.now() < GOOGLE_OAUTH_CACHE.exp) return GOOGLE_OAUTH_CACHE.token;
  const refresh = await appSettingGet(env, "google_refresh_token");
  if (!refresh) return null;
  const r = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({ client_id: env.GOOGLE_OAUTH_CLIENT_ID, client_secret: env.GOOGLE_OAUTH_CLIENT_SECRET, refresh_token: refresh, grant_type: "refresh_token" }).toString()
  });
  const d = await r.json().catch(() => null);
  if (!d || !d.access_token) return null;
  GOOGLE_OAUTH_CACHE = { token: d.access_token, exp: Date.now() + ((d.expires_in || 3600) - 120) * 1e3 };
  return d.access_token;
}
__name(googleOAuthAccessToken, "googleOAuthAccessToken");
__name2(googleOAuthAccessToken, "googleOAuthAccessToken");
async function handleGoogleOAuthStart(request, env, url) {
  const who = await contactAuth(env, url.searchParams.get("ns_token"), url.searchParams.get("ns_email"), url.searchParams.get("ns_server"));
  if (!who || who.forbidden || !who.admin) return new Response("Tylko admin mo\u017Ce po\u0142\u0105czy\u0107 Kalendarz Google (zaloguj si\u0119 ponownie w panelu).", { status: 403, headers: { "Content-Type": "text/plain; charset=utf-8" } });
  if (!env.GOOGLE_OAUTH_CLIENT_ID || !env.GOOGLE_OAUTH_CLIENT_SECRET) return new Response("Brak sekret\xF3w GOOGLE_OAUTH_CLIENT_ID / GOOGLE_OAUTH_CLIENT_SECRET w workerze.", { status: 500, headers: { "Content-Type": "text/plain; charset=utf-8" } });
  await ensureGoogleTables(env);
  const state = crypto.randomUUID();
  await env.DB.prepare("INSERT INTO oauth_states (state, email, created_at) VALUES (?, ?, ?)").bind(state, who.email, Math.floor(Date.now() / 1e3)).run();
  const q = new URLSearchParams({ client_id: env.GOOGLE_OAUTH_CLIENT_ID, redirect_uri: googleOAuthRedirectUri(url), response_type: "code", scope: GOOGLE_OAUTH_SCOPE, access_type: "offline", prompt: "consent", include_granted_scopes: "true", state });
  return Response.redirect("https://accounts.google.com/o/oauth2/v2/auth?" + q.toString(), 302);
}
__name(handleGoogleOAuthStart, "handleGoogleOAuthStart");
__name2(handleGoogleOAuthStart, "handleGoogleOAuthStart");
async function handleGoogleOAuthCallback(request, env, url) {
  const page = /* @__PURE__ */ __name((msg, ok) => new Response(`<!doctype html><meta charset="utf-8"><body style="font-family:sans-serif;max-width:520px;margin:60px auto;text-align:center"><h2>${ok ? "\u2705" : "\u274C"} ${msg}</h2><p>Mo\u017Cesz zamkn\u0105\u0107 to okno i wr\xF3ci\u0107 do panelu trenera.</p></body>`, { status: ok ? 200 : 400, headers: { "Content-Type": "text/html; charset=utf-8" } }), "page");
  await ensureGoogleTables(env);
  const state = url.searchParams.get("state"), code = url.searchParams.get("code");
  if (url.searchParams.get("error")) return page("Google odrzuci\u0142 po\u0142\u0105czenie: " + url.searchParams.get("error"), false);
  const st = state ? await env.DB.prepare("SELECT * FROM oauth_states WHERE state = ?").bind(state).first() : null;
  if (!st || Math.floor(Date.now() / 1e3) - st.created_at > 900) return page("Link wygas\u0142 \u2014 kliknij \u201EPo\u0142\u0105cz Kalendarz Google\u201D w panelu jeszcze raz.", false);
  await env.DB.prepare("DELETE FROM oauth_states WHERE state = ? OR created_at < ?").bind(state, Math.floor(Date.now() / 1e3) - 3600).run();
  const r = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({ code, client_id: env.GOOGLE_OAUTH_CLIENT_ID, client_secret: env.GOOGLE_OAUTH_CLIENT_SECRET, redirect_uri: googleOAuthRedirectUri(url), grant_type: "authorization_code" }).toString()
  });
  const d = await r.json().catch(() => null);
  if (!d || !d.refresh_token) return page("Nie otrzymano tokenu od Google" + (d && d.error_description ? ": " + d.error_description : "") + ".", false);
  await appSettingSet(env, "google_refresh_token", d.refresh_token);
  await appSettingSet(env, "google_scopes", d.scope || "");
  GOOGLE_OAUTH_CACHE = { token: d.access_token || null, exp: d.access_token ? Date.now() + ((d.expires_in || 3600) - 120) * 1e3 : 0 };
  let account = "";
  try {
    const cr = await fetch("https://www.googleapis.com/calendar/v3/calendars/primary", { headers: { "Authorization": "Bearer " + d.access_token } });
    const cd = await cr.json().catch(() => null);
    account = cd && cd.id ? cd.id : "";
  } catch (e) {
  }
  await appSettingSet(env, "google_account", account);
  await appSettingSet(env, "google_connected_by", st.email || "");
  return page("Kalendarz Google po\u0142\u0105czony" + (account ? ": " + account : ""), true);
}
__name(handleGoogleOAuthCallback, "handleGoogleOAuthCallback");
__name2(handleGoogleOAuthCallback, "handleGoogleOAuthCallback");
async function handleGoogleOAuthStatus(request, env, url) {
  const who = await contactAuth(env, url.searchParams.get("ns_token"), url.searchParams.get("ns_email"), url.searchParams.get("ns_server"));
  if (!who) return json({ error: "Nieprawid\u0142owy lub wygas\u0142y token NS \u2014 zaloguj si\u0119 ponownie" }, 401);
  if (who.forbidden) return json({ error: "Tylko trener/admin" }, 403);
  await ensureGoogleTables(env);
  const refresh = await appSettingGet(env, "google_refresh_token");
  const tok = refresh ? await googleOAuthAccessToken(env) : null;
  return json({
    ok: true,
    secrets_ok: !!(env.GOOGLE_OAUTH_CLIENT_ID && env.GOOGLE_OAUTH_CLIENT_SECRET),
    connected: !!refresh,
    token_ok: !!tok,
    account: await appSettingGet(env, "google_account"),
    connected_by: await appSettingGet(env, "google_connected_by"),
    redirect_uri: googleOAuthRedirectUri(url),
    cc: callDaysCc(env),
    gmail_ok: /gmail\.send/.test(await appSettingGet(env, "google_scopes") || "")
  });
}
__name(handleGoogleOAuthStatus, "handleGoogleOAuthStatus");
__name2(handleGoogleOAuthStatus, "handleGoogleOAuthStatus");
async function handleGoogleOAuthDisconnect(request, env) {
  let body = {};
  try {
    body = await request.json();
  } catch (e) {
  }
  const who = await contactAuth(env, body.ns_token, body.ns_email, body.ns_server);
  if (!who || who.forbidden || !who.admin) return json({ error: "Tylko admin" }, 403);
  await ensureGoogleTables(env);
  await env.DB.prepare("DELETE FROM app_settings WHERE key IN ('google_refresh_token','google_account','google_connected_by','google_scopes')").run();
  GOOGLE_OAUTH_CACHE = { token: null, exp: 0 };
  return json({ ok: true });
}
__name(handleGoogleOAuthDisconnect, "handleGoogleOAuthDisconnect");
__name2(handleGoogleOAuthDisconnect, "handleGoogleOAuthDisconnect");
function b64Utf8(str) {
  const bytes = new TextEncoder().encode(str);
  let bin = "";
  for (let i = 0; i < bytes.length; i += 32768) bin += String.fromCharCode.apply(null, bytes.subarray(i, i + 32768));
  return btoa(bin);
}
__name(b64Utf8, "b64Utf8");
function mimeWord(str) {
  return /^[\x20-\x7e]*$/.test(str) ? str : "=?UTF-8?B?" + b64Utf8(str) + "?=";
}
__name(mimeWord, "mimeWord");
function wrap76(b64) {
  return b64.replace(/.{1,76}/g, "$&\r\n").trim();
}
__name(wrap76, "wrap76");
async function handleEmailSendPlan(request, env) {
  let body = {};
  try {
    body = await request.json();
  } catch (e) {
  }
  const who = await contactAuth(env, body.ns_token, body.ns_email, body.ns_server);
  if (!who) return json({ error: "Nieprawid\u0142owy lub wygas\u0142y token NS \u2014 zaloguj si\u0119 ponownie" }, 401);
  if (who.forbidden) return json({ error: "Tylko trener/admin" }, 403);
  const to = await nsUserEmailById(env, body.student_id);
  if (!to || !/@/.test(to)) return json({ error: "Kursant nie ma adresu e-mail w NS" }, 400);
  const pdf = String(body.pdf_base64 || "").replace(/\s+/g, "");
  if (!pdf || pdf.length > 14e6) return json({ error: "Brak PDF albo plik jest za du\u017Cy" }, 400);
  const token = await googleOAuthAccessToken(env);
  if (!token) return json({ error: "Konto Google nie jest po\u0142\u0105czone \u2014 admin: \u2699\uFE0F Admin \u2192 Kalendarz Google \u2192 Po\u0142\u0105cz" }, 400);
  const account = await appSettingGet(env, "google_account") || "";
  const subject = String(body.subject || "Plan \u0107wicze\u0144").slice(0, 200);
  const filename = String(body.filename || "plan.pdf").replace(/[^\w.\-ąćęłńóśźżĄĆĘŁŃÓŚŹŻ ]/g, "_").slice(0, 120);
  const boundary = "nsplan_" + crypto.randomUUID().replace(/-/g, "");
  const headers = [
    "From: " + mimeWord("Nowa Mowa") + (account ? " <" + account + ">" : ""),
    "To: " + to,
    body.cc_me ? "Cc: " + who.email : null,
    "Reply-To: " + who.email,
    "Subject: " + mimeWord(subject),
    "MIME-Version: 1.0",
    'Content-Type: multipart/mixed; boundary="' + boundary + '"'
  ].filter(Boolean);
  const mime = headers.join("\r\n") + "\r\n\r\n--" + boundary + "\r\nContent-Type: text/html; charset=UTF-8\r\nContent-Transfer-Encoding: base64\r\n\r\n" + wrap76(b64Utf8(String(body.html || ""))) + "\r\n--" + boundary + '\r\nContent-Type: application/pdf; name="' + mimeWord(filename) + '"\r\nContent-Disposition: attachment; filename="' + mimeWord(filename) + '"\r\nContent-Transfer-Encoding: base64\r\n\r\n' + wrap76(pdf) + "\r\n--" + boundary + "--";
  const r = await fetch("https://gmail.googleapis.com/upload/gmail/v1/users/me/messages/send?uploadType=media", {
    method: "POST",
    headers: { "Authorization": "Bearer " + token, "Content-Type": "message/rfc822" },
    body: mime
  });
  const d = await r.json().catch(() => ({}));
  if (!r.ok) {
    const msg = d && d.error && d.error.message || "status " + r.status;
    if (/has not been used|is disabled|SERVICE_DISABLED|accessNotConfigured/i.test(msg)) {
      const m = msg.match(/https:\/\/console\.developers\.google\.com\/\S+?(?=\s|$)/);
      return json({ error: "W projekcie Google Cloud nie jest w\u0142\u0105czone Gmail API. Admin: otw\xF3rz " + (m ? m[0] : "Google Cloud \u2192 APIs & Services \u2192 Library \u2192 Gmail API") + " i kliknij \u201EEnable\u201D, odczekaj kilka minut i wy\u015Blij ponownie." }, 400);
    }
    if (r.status === 403 || /insufficient|scope/i.test(msg)) return json({ error: "Po\u0142\u0105czone konto Google nie ma zgody na wysy\u0142anie maili \u2014 admin: \u2699\uFE0F Admin \u2192 Kalendarz Google \u2192 Roz\u0142\u0105cz i po\u0142\u0105cz ponownie (zaakceptuj wysy\u0142anie e-maili). Szczeg\xF3\u0142y: " + msg }, 400);
    return json({ error: "Gmail: " + msg }, 502);
  }
  try {
    await env.DB.prepare("CREATE TABLE IF NOT EXISTS email_log (id INTEGER PRIMARY KEY AUTOINCREMENT, ns_student_id TEXT, to_email TEXT, subject TEXT, sent_by TEXT, gmail_id TEXT, created_at INTEGER)").run();
    await env.DB.prepare("INSERT INTO email_log (ns_student_id, to_email, subject, sent_by, gmail_id, created_at) VALUES (?, ?, ?, ?, ?, ?)").bind(String(body.student_id), to, subject, who.email, d.id || null, Math.floor(Date.now() / 1e3)).run();
  } catch (e) {
  }
  return json({ ok: true, to, id: d.id || null, from: account });
}
__name(handleEmailSendPlan, "handleEmailSendPlan");
__name2(handleEmailSendPlan, "handleEmailSendPlan");
var SPECIAL_TASKS_READY = false;
var SPECIAL_TASK_COUNT = 37;
async function ensureSpecialTasksTable(env) {
  if (SPECIAL_TASKS_READY) return;
  await env.DB.prepare(`CREATE TABLE IF NOT EXISTS special_task_done (id INTEGER PRIMARY KEY AUTOINCREMENT, ns_student_id TEXT NOT NULL, code TEXT NOT NULL, source TEXT, note TEXT, credited_by TEXT, done_at INTEGER, UNIQUE(ns_student_id, code))`).run();
  SPECIAL_TASKS_READY = true;
}
__name(ensureSpecialTasksTable, "ensureSpecialTasksTable");
async function handleSpecialTasksList(request, env, url) {
  const who = await contactAuth(env, url.searchParams.get("ns_token"), url.searchParams.get("ns_email"), url.searchParams.get("ns_server"));
  if (!who) return json({ error: "Nieprawid\u0142owy lub wygas\u0142y token NS \u2014 zaloguj si\u0119 ponownie" }, 401);
  if (who.forbidden) return json({ error: "Tylko trener/admin" }, 403);
  const sid = url.searchParams.get("student_id");
  if (!sid) return json({ error: "Brak student_id" }, 400);
  await ensureSpecialTasksTable(env);
  const rows = (await env.DB.prepare("SELECT code, source, note, credited_by, done_at FROM special_task_done WHERE ns_student_id = ? ORDER BY done_at DESC").bind(String(sid)).all()).results || [];
  return json({ ok: true, done: rows });
}
__name(handleSpecialTasksList, "handleSpecialTasksList");
__name2(handleSpecialTasksList, "handleSpecialTasksList");
async function handleSpecialTasksCredit(request, env, undo) {
  let body = {};
  try {
    body = await request.json();
  } catch (e) {
  }
  const who = await contactAuth(env, body.ns_token, body.ns_email, body.ns_server);
  if (!who) return json({ error: "Nieprawid\u0142owy lub wygas\u0142y token NS \u2014 zaloguj si\u0119 ponownie" }, 401);
  if (who.forbidden) return json({ error: "Tylko trener/admin" }, 403);
  const sid = String(body.student_id || ""), code = String(body.code || "").toUpperCase();
  const num = /^ZS(\d{2})$/.test(code) ? parseInt(code.slice(2), 10) : 0;
  if (!sid || num < 1 || num > SPECIAL_TASK_COUNT) return json({ error: "Brak kursanta albo z\u0142y kod zadania (ZS01\u2013ZS" + SPECIAL_TASK_COUNT + ")" }, 400);
  await ensureSpecialTasksTable(env);
  if (undo) {
    await env.DB.prepare("DELETE FROM special_task_done WHERE ns_student_id = ? AND code = ?").bind(sid, code).run();
    return json({ ok: true });
  }
  const now = Math.floor(Date.now() / 1e3);
  await env.DB.prepare(`INSERT INTO special_task_done (ns_student_id, code, source, note, credited_by, done_at) VALUES (?, ?, ?, ?, ?, ?)
    ON CONFLICT(ns_student_id, code) DO UPDATE SET source = excluded.source, note = excluded.note, credited_by = excluded.credited_by, done_at = excluded.done_at`).bind(sid, code, String(body.source || "").slice(0, 40), String(body.note || "").slice(0, 500), who.email, body.done_at ? Number(body.done_at) : now).run();
  return json({ ok: true, code, done_at: body.done_at ? Number(body.done_at) : now });
}
__name(handleSpecialTasksCredit, "handleSpecialTasksCredit");
__name2(handleSpecialTasksCredit, "handleSpecialTasksCredit");
async function handleSpecialTasksMine(request, env, url) {
  const nsEmail = url.searchParams.get("ns_email");
  const valid = await validateNsToken(url.searchParams.get("ns_token"), nsEmail, url.searchParams.get("ns_server"));
  if (!valid) return json({ error: "Nieprawid\u0142owy lub wygas\u0142y token NS \u2014 zaloguj si\u0119 ponownie" }, 401);
  const u = await nsUserByEmail(env, nsEmail);
  if (!u) return json({ ok: true, done: [] });
  await ensureSpecialTasksTable(env);
  const rows = (await env.DB.prepare("SELECT code, source, note, done_at FROM special_task_done WHERE ns_student_id = ? ORDER BY done_at DESC").bind(String(u.id)).all()).results || [];
  return json({ ok: true, done: rows });
}
__name(handleSpecialTasksMine, "handleSpecialTasksMine");
__name2(handleSpecialTasksMine, "handleSpecialTasksMine");
var COACH_TABLES_READY = false;
async function ensureCoachTables(env) {
  if (COACH_TABLES_READY) return;
  await env.DB.batch([
    env.DB.prepare(`CREATE TABLE IF NOT EXISTS review_log (id INTEGER PRIMARY KEY AUTOINCREMENT, ns_record_id TEXT NOT NULL, ns_student_id TEXT, trainer_email TEXT, is_correct INTEGER, weak TEXT, created_at INTEGER)`),
    env.DB.prepare(`CREATE INDEX IF NOT EXISTS idx_review_log_record ON review_log(ns_record_id)`),
    env.DB.prepare(`CREATE TABLE IF NOT EXISTS call_notice (id INTEGER PRIMARY KEY AUTOINCREMENT, ns_student_id TEXT NOT NULL, ns_record_id TEXT, criteria TEXT, correction_no INTEGER, created_by TEXT, created_at INTEGER, acked_at INTEGER, cleared_at INTEGER, cleared_by TEXT)`),
    env.DB.prepare(`CREATE INDEX IF NOT EXISTS idx_call_notice_student ON call_notice(ns_student_id)`),
    env.DB.prepare(`CREATE TABLE IF NOT EXISTS student_focus (ns_student_id TEXT PRIMARY KEY, criteria TEXT, note TEXT, achieved TEXT, set_by TEXT, set_at INTEGER)`),
    env.DB.prepare(`CREATE TABLE IF NOT EXISTS coach_settings (trainer_email TEXT PRIMARY KEY, corr_threshold INTEGER, updated_at INTEGER)`),
    env.DB.prepare(`CREATE TABLE IF NOT EXISTS student_coach_settings (ns_student_id TEXT PRIMARY KEY, corr_threshold INTEGER, set_by TEXT, updated_at INTEGER)`),
    env.DB.prepare(`CREATE TABLE IF NOT EXISTS record_fp (ns_record_id TEXT PRIMARY KEY, ns_student_id TEXT, sha256 TEXT, dur REAL, rec_ts INTEGER, env TEXT, created_at INTEGER)`),
    env.DB.prepare(`CREATE INDEX IF NOT EXISTS idx_record_fp_student ON record_fp(ns_student_id)`),
    env.DB.prepare(`CREATE TABLE IF NOT EXISTS record_system_override (ns_record_id TEXT PRIMARY KEY, ns_student_id TEXT, system TEXT NOT NULL, student_system TEXT, set_by TEXT, set_at INTEGER)`)
  ]);
  COACH_TABLES_READY = true;
}
__name(ensureCoachTables, "ensureCoachTables");
async function coachStudentFromRequest(request, env, url, body) {
  body = body || {};
  const auth = request.headers.get("Authorization") || "";
  const token = url && url.searchParams.get("ns_token") || body.ns_token || (auth.match(/token=([^\s,]+)/i) || [])[1] || "";
  const email = url && url.searchParams.get("ns_email") || body.ns_email || request.headers.get("Email") || request.headers.get("X-NS-Email") || "";
  const server = url && url.searchParams.get("ns_server") || body.ns_server || request.headers.get("X-NS-Server") || "new";
  if (!token || !email) return null;
  const valid = await validateNsToken(token, email, server);
  if (!valid) return null;
  const u = await nsUserByEmail(env, email);
  return u ? { id: String(u.id), email } : { id: null, email };
}
__name(coachStudentFromRequest, "coachStudentFromRequest");
function coachJsonArr(v) {
  try {
    const a = JSON.parse(v || "[]");
    return Array.isArray(a) ? a : [];
  } catch (e) {
    return [];
  }
}
__name(coachJsonArr, "coachJsonArr");
async function handleReviewLog(request, env) {
  let body = {};
  try {
    body = await request.json();
  } catch (e) {
  }
  const who = await contactAuth(env, body.ns_token, body.ns_email, body.ns_server);
  if (!who) return json({ error: "Nieprawid\u0142owy lub wygas\u0142y token NS \u2014 zaloguj si\u0119 ponownie" }, 401);
  if (who.forbidden) return json({ error: "Tylko trener/admin" }, 403);
  const rid = String(body.record_id || ""), sid = String(body.student_id || "");
  if (!rid) return json({ error: "Brak record_id" }, 400);
  await ensureCoachTables(env);
  const now = Math.floor(Date.now() / 1e3);
  const ok = body.is_correct ? 1 : 0;
  const weak = (Array.isArray(body.weak) ? body.weak : []).map((x) => String(x).slice(0, 60)).slice(0, 12);
  await env.DB.prepare("INSERT INTO review_log (ns_record_id, ns_student_id, trainer_email, is_correct, weak, created_at) VALUES (?, ?, ?, ?, ?, ?)").bind(rid, sid || null, who.email, ok, JSON.stringify(weak), now).run();
  const cnt = await env.DB.prepare("SELECT COUNT(*) AS n FROM review_log WHERE ns_record_id = ? AND is_correct = 0").bind(rid).first();
  const correctionNo = cnt ? Number(cnt.n) : 0;
  let notice = null;
  if (ok) {
    await env.DB.prepare("UPDATE call_notice SET cleared_at = ?, cleared_by = ? WHERE ns_record_id = ? AND cleared_at IS NULL").bind(now, who.email, rid).run();
  } else {
    const threshold = await coachThresholdFor(env, sid, who.email, body.threshold);
    if (sid && correctionNo >= threshold) {
      const existing = await env.DB.prepare("SELECT id FROM call_notice WHERE ns_record_id = ? AND cleared_at IS NULL").bind(rid).first();
      if (existing) {
        await env.DB.prepare("UPDATE call_notice SET criteria = ?, correction_no = ?, created_at = ?, acked_at = NULL WHERE id = ?").bind(JSON.stringify(weak), correctionNo, now, existing.id).run();
        notice = { id: existing.id };
      } else {
        const r = await env.DB.prepare("INSERT INTO call_notice (ns_student_id, ns_record_id, criteria, correction_no, created_by, created_at) VALUES (?, ?, ?, ?, ?, ?)").bind(sid, rid, JSON.stringify(weak), correctionNo, who.email, now).run();
        notice = { id: r.meta.last_row_id };
      }
    }
  }
  return json({ ok: true, correction_no: correctionNo, notice });
}
__name(handleReviewLog, "handleReviewLog");
__name2(handleReviewLog, "handleReviewLog");
async function coachThresholdFor(env, sid, trainerEmail, fallback) {
  let v = null;
  if (sid) {
    const s1 = await env.DB.prepare("SELECT corr_threshold FROM student_coach_settings WHERE ns_student_id = ?").bind(String(sid)).first();
    if (s1 && s1.corr_threshold) v = s1.corr_threshold;
  }
  if (!v && trainerEmail) {
    const s2 = await env.DB.prepare("SELECT corr_threshold FROM coach_settings WHERE trainer_email = ?").bind(String(trainerEmail).toLowerCase()).first();
    if (s2 && s2.corr_threshold) v = s2.corr_threshold;
  }
  if (!v) v = parseInt(fallback, 10) || 3;
  return Math.max(2, Math.min(10, v));
}
__name(coachThresholdFor, "coachThresholdFor");
__name2(coachThresholdFor, "coachThresholdFor");
async function handleCoachSettingsGet(request, env, url) {
  const who = await contactAuth(env, url.searchParams.get("ns_token"), url.searchParams.get("ns_email"), url.searchParams.get("ns_server"));
  if (!who) return json({ error: "Nieprawid\u0142owy lub wygas\u0142y token NS \u2014 zaloguj si\u0119 ponownie" }, 401);
  if (who.forbidden) return json({ error: "Tylko trener/admin" }, 403);
  await ensureCoachTables(env);
  const sid = url.searchParams.get("student_id");
  if (sid) {
    const own = await env.DB.prepare("SELECT corr_threshold, set_by, updated_at FROM student_coach_settings WHERE ns_student_id = ?").bind(String(sid)).first();
    const thr = await coachThresholdFor(env, sid, who.email, 3);
    return json({ ok: true, settings: { student_id: String(sid), corr_threshold: thr, student_own: !!(own && own.corr_threshold), set_by: own ? own.set_by : null, updated_at: own ? own.updated_at : null } });
  }
  const r = await env.DB.prepare("SELECT corr_threshold, updated_at FROM coach_settings WHERE trainer_email = ?").bind(String(who.email).toLowerCase()).first();
  return json({ ok: true, settings: { corr_threshold: r && r.corr_threshold || 3, updated_at: r ? r.updated_at : null } });
}
__name(handleCoachSettingsGet, "handleCoachSettingsGet");
__name2(handleCoachSettingsGet, "handleCoachSettingsGet");
async function handleCoachSettingsSave(request, env) {
  let body = {};
  try {
    body = await request.json();
  } catch (e) {
  }
  const who = await contactAuth(env, body.ns_token, body.ns_email, body.ns_server);
  if (!who) return json({ error: "Nieprawid\u0142owy lub wygas\u0142y token NS \u2014 zaloguj si\u0119 ponownie" }, 401);
  if (who.forbidden) return json({ error: "Tylko trener/admin" }, 403);
  const t = parseInt(body.corr_threshold, 10);
  if (!(t >= 2 && t <= 10)) return json({ error: "Pr\xF3g musi by\u0107 liczb\u0105 od 2 do 10" }, 400);
  await ensureCoachTables(env);
  const now = Math.floor(Date.now() / 1e3);
  if (body.student_id) {
    const sid = String(body.student_id);
    await env.DB.prepare(`INSERT INTO student_coach_settings (ns_student_id, corr_threshold, set_by, updated_at) VALUES (?, ?, ?, ?)
      ON CONFLICT(ns_student_id) DO UPDATE SET corr_threshold = excluded.corr_threshold, set_by = excluded.set_by, updated_at = excluded.updated_at`).bind(sid, t, String(who.email).toLowerCase(), now).run();
    return json({ ok: true, settings: { student_id: sid, corr_threshold: t, student_own: true, set_by: String(who.email).toLowerCase(), updated_at: now } });
  }
  await env.DB.prepare(`INSERT INTO coach_settings (trainer_email, corr_threshold, updated_at) VALUES (?, ?, ?)
    ON CONFLICT(trainer_email) DO UPDATE SET corr_threshold = excluded.corr_threshold, updated_at = excluded.updated_at`).bind(String(who.email).toLowerCase(), t, now).run();
  return json({ ok: true, settings: { corr_threshold: t, updated_at: now } });
}
__name(handleCoachSettingsSave, "handleCoachSettingsSave");
__name2(handleCoachSettingsSave, "handleCoachSettingsSave");
var WHISPER_MODEL = "@cf/openai/whisper-large-v3-turbo";
var TRANSCRIPT_TABLES_READY = false;
async function ensureTranscriptTables(env) {
  if (TRANSCRIPT_TABLES_READY) return;
  await env.DB.batch([
    env.DB.prepare(`CREATE TABLE IF NOT EXISTS record_transcript (ns_record_id TEXT PRIMARY KEY, ns_student_id TEXT, status TEXT, text TEXT, lang TEXT, dur REAL, words TEXT, model TEXT, error TEXT, created_at INTEGER)`),
    env.DB.prepare(`CREATE INDEX IF NOT EXISTS idx_record_transcript_student ON record_transcript(ns_student_id)`),
    env.DB.prepare(`CREATE TABLE IF NOT EXISTS transcript_usage (day TEXT PRIMARY KEY, minutes REAL)`),
    env.DB.prepare(`CREATE TABLE IF NOT EXISTS diar_usage (day TEXT PRIMARY KEY, minutes REAL)`),
    env.DB.prepare(`CREATE TABLE IF NOT EXISTS tx_kv (k TEXT PRIMARY KEY, v TEXT)`)
  ]);
  try {
    await env.DB.prepare("ALTER TABLE record_transcript ADD COLUMN speakers TEXT").run();
  } catch (e) {
  }
  try {
    await env.DB.prepare("ALTER TABLE record_transcript ADD COLUMN diar TEXT").run();
  } catch (e) {
  }
  try {
    await env.DB.prepare("ALTER TABLE record_transcript ADD COLUMN spk_names TEXT").run();
  } catch (e) {
  }
  try {
    await env.DB.prepare("ALTER TABLE record_transcript ADD COLUMN spk_src TEXT").run();
  } catch (e) {
  }
  try {
    await env.DB.prepare("CREATE TABLE IF NOT EXISTS llm_usage (day TEXT PRIMARY KEY, calls INTEGER)").run();
  } catch (e) {
  }
  try {
    await env.DB.prepare("ALTER TABLE record_transcript ADD COLUMN summary TEXT").run();
  } catch (e) {
  }
  try {
    await env.DB.prepare("ALTER TABLE record_transcript ADD COLUMN fixed TEXT").run();
  } catch (e) {
  }
  try {
    await env.DB.prepare("ALTER TABLE record_transcript ADD COLUMN spk_people TEXT").run();
  } catch (e) {
  }
  try {
    await env.DB.prepare("ALTER TABLE record_transcript ADD COLUMN edited_by TEXT").run();
  } catch (e) {
  }
  try {
    await env.DB.prepare("ALTER TABLE record_transcript ADD COLUMN src_file TEXT").run();
  } catch (e) {
  }
  try {
    await env.DB.prepare("CREATE TABLE IF NOT EXISTS trainer_uploader (ns_user_id TEXT PRIMARY KEY, set_by TEXT, created_at INTEGER)").run();
  } catch (e) {
  }
  TRANSCRIPT_TABLES_READY = true;
}
__name(ensureTranscriptTables, "ensureTranscriptTables");
__name2(ensureTranscriptTables, "ensureTranscriptTables");
function transcriptDailyLimit(env) {
  const v = parseFloat(env.TRANSCRIBE_DAILY_MIN);
  return v > 0 ? v : 1500;
}
__name(transcriptDailyLimit, "transcriptDailyLimit");
var TX_VERSION = "v5-diar";
var DIAR_MODEL = "@cf/deepgram/nova-3";
function diarDailyLimit(env) {
  const v = parseFloat(env.DIARIZE_DAILY_MIN);
  return isFinite(v) && v >= 0 ? v : env.DEEPGRAM_API_KEY ? 1500 : 300;
}
__name(diarDailyLimit, "diarDailyLimit");
function diarEnabled(env) {
  return !!(env.DEEPGRAM_API_KEY || env.DIARIZE_NOVA);
}
__name(diarEnabled, "diarEnabled");
async function deepgramWords(env, audioBuf, lang) {
  const call = /* @__PURE__ */ __name(async (diarParams) => {
    const q = new URLSearchParams({ model: "nova-3", punctuate: "true", ...diarParams });
    if (lang) q.set("language", lang);
    else q.set("detect_language", "true");
    const r2 = await fetch("https://api.deepgram.com/v1/listen?" + q.toString(), {
      method: "POST",
      headers: { "Authorization": "Token " + env.DEEPGRAM_API_KEY, "Content-Type": "audio/mpeg" },
      body: audioBuf
    });
    return { r: r2, d: await r2.json().catch(() => null) };
  }, "call");
  let { r, d } = await call({ diarize: "true", diarize_model: "latest" });
  if (r.status === 400) ({ r, d } = await call({ diarize: "true" }));
  if (!r.ok) throw new Error("Deepgram " + r.status + ": " + String(d && (d.err_msg || d.message) || "").slice(0, 120));
  const ch = d && d.results && d.results.channels && d.results.channels[0];
  return { words: ch && ch.alternatives && ch.alternatives[0] && ch.alternatives[0].words || [], dur: Number(d && d.metadata && d.metadata.duration) || 0 };
}
__name(deepgramWords, "deepgramWords");
__name2(deepgramWords, "deepgramWords");
function txIsMonolog(cat) {
  return /monolog|czytani|podsumowanie dnia/i.test(String(cat || ""));
}
__name(txIsMonolog, "txIsMonolog");
async function diarizeWords(env, audioBuf, lang, words) {
  if (env.DEEPGRAM_API_KEY) {
    const g = await deepgramWords(env, audioBuf, lang);
    const nwD = g.words.filter((w) => w && w.speaker !== void 0 && w.speaker !== null);
    if (!nwD.length) return { speakers: null, dur: g.dur, note: "Deepgram: brak s\u0142\xF3w" };
    const m = mapSpeakers(nwD, words, g.dur);
    const raw = new Set(nwD.map((w) => w.speaker)).size;
    m.note = m.speakers ? "OK (Deepgram), g\u0142osy: " + new Set(m.speakers).size : "Deepgram us\u0142ysza\u0142 " + (raw > 1 ? raw + " g\u0142osy, ale prawie wszystko m\xF3wi jedna osoba" : "jedn\u0105 osob\u0119") + " (s\u0142\xF3w: " + nwD.length + ")";
    return m;
  }
  const mkStream = /* @__PURE__ */ __name((fixed) => {
    if (fixed && typeof FixedLengthStream !== "undefined") {
      const fl = new FixedLengthStream(audioBuf.byteLength);
      const w = fl.writable.getWriter();
      w.write(new Uint8Array(audioBuf)).then(() => w.close()).catch(() => {
      });
      return fl.readable;
    }
    return new Response(audioBuf.slice(0)).body;
  }, "mkStream");
  const tries = [[true, true], [false, true], [true, false], [false, false]];
  let res = null, nw = [], dur = 0;
  const notes = [];
  for (const [fixed, withLang] of tries) {
    const tag = (fixed ? "fix" : "str") + (withLang ? "+" + (lang || "-") : "+auto");
    try {
      const inp = { audio: { body: mkStream(fixed), contentType: "audio/mpeg" }, diarize: true, punctuate: true };
      if (withLang && lang) inp.language = lang;
      else inp.detect_language = true;
      res = await env.AI.run(DIAR_MODEL, inp);
      const ch = res && res.results && res.results.channels && res.results.channels[0];
      const all = ch && ch.alternatives && ch.alternatives[0] && ch.alternatives[0].words || [];
      nw = all.filter((w) => w && w.speaker !== void 0 && w.speaker !== null);
      dur = Math.max(dur, Number(res && res.metadata && res.metadata.duration) || 0);
      if (nw.length) break;
      notes.push(tag + ": " + (all.length ? "s\u0142owa bez m\xF3wcy" : "pusto"));
    } catch (e) {
      notes.push(tag + ": " + String(e && e.message || e).slice(0, 80));
    }
  }
  if (!nw.length) {
    if (!res) throw new Error(notes.join(" | "));
    return { speakers: null, dur, note: "Nova: brak s\u0142\xF3w \u2014 " + notes.join(" | ") };
  }
  return mapSpeakers(nw, words, dur);
}
__name(diarizeWords, "diarizeWords");
__name2(diarizeWords, "diarizeWords");
function mapSpeakers(nw, words, dur) {
  const mids = nw.map((w) => ((Number(w.start) || 0) + (Number(w.end) || 0)) / 2);
  const sp = [];
  let j = 0, last = Number(nw[0].speaker);
  for (const w of words) {
    const m = (w[1] + w[2]) / 2;
    while (j + 1 < mids.length && Math.abs(mids[j + 1] - m) <= Math.abs(mids[j] - m)) j++;
    if (Math.abs(mids[j] - m) <= 1.5) last = Number(nw[j].speaker);
    sp.push(last);
  }
  const n = new Set(sp).size;
  return { speakers: n >= 2 ? sp : null, dur, note: n >= 2 ? "OK, g\u0142osy: " + n : "Nova: jeden g\u0142os" };
}
__name(mapSpeakers, "mapSpeakers");
__name2(mapSpeakers, "mapSpeakers");
function txLangForCountry(c) {
  c = String(c || "").trim().toUpperCase();
  if (!c) return null;
  if (c === "PL" || c === "POLSKA" || c === "POLAND") return "pl";
  if (c === "CZ" || c === "CZECH" || c === "CZECHY") return "cs";
  if (c === "SK" || c === "SLOVAKIA" || c === "S\u0141OWACJA") return "sk";
  if (c === "GB" || c === "UK" || c === "IE" || c === "US") return "en";
  if (c === "DE" || c === "AT" || c === "CH") return "de";
  return null;
}
__name(txLangForCountry, "txLangForCountry");
__name2(txLangForCountry, "txLangForCountry");
var TX_PROMPT = {
  pl: "Rozmowa po polsku, np. w sklepie albo przez telefon. Dzie\u0144 dobry, poprosz\u0119.",
  cs: "Rozhovor v \u010De\u0161tin\u011B, nap\u0159\xEDklad v obchod\u011B nebo po telefonu. Dobr\xFD den, pros\xEDm.",
  sk: "Rozhovor po slovensky, napr\xEDklad v obchode alebo po telef\xF3ne. Dobr\xFD de\u0148, pros\xEDm.",
  en: "A conversation in English, e.g. in a shop or on the phone."
};
function txSegmentOk(seg, prevTexts) {
  const t = String(seg.text || "").trim();
  if (!t) return false;
  const nsp = Number(seg.no_speech_prob), lp = Number(seg.avg_logprob), cr = Number(seg.compression_ratio);
  if (isFinite(nsp) && isFinite(lp) && nsp > 0.5 && lp < -0.8) return false;
  if (isFinite(lp) && lp < -1.2) return false;
  if (isFinite(cr) && cr > 2.4) return false;
  const k = t.toLowerCase().replace(/[^\p{L}\p{N} ]/gu, "").trim();
  if (k && prevTexts.length >= 2 && prevTexts[prevTexts.length - 1] === k && prevTexts[prevTexts.length - 2] === k) return false;
  prevTexts.push(k);
  return true;
}
__name(txSegmentOk, "txSegmentOk");
__name2(txSegmentOk, "txSegmentOk");
var TX_PATH_DEFAULT = "/uploads/record/record_file/{uslug}/{cslug}/{file}";
var TX_PATH_SQL_COLS = `to_jsonb(u) ->> 'slug' AS p_uslug, u.first_name AS p_fn, u.last_name AS p_ln, u.email AS p_email, to_jsonb(rc) ->> 'slug' AS p_cslug_db, rc.name AS p_cname, rct.name AS p_cname_pl`;
function txSlugify(t) {
  return String(t || "").replace(/ł/g, "l").replace(/Ł/g, "L").normalize("NFD").replace(/[̀-ͯ]/g, "").toLowerCase().replace(/[^a-z0-9]+/g, "-").replace(/^-+|-+$/g, "");
}
__name(txSlugify, "txSlugify");
__name2(txSlugify, "txSlugify");
function txPathVars(row, rid, uid, file) {
  const gen = (row.p_fn || row.p_ln) && uid ? txSlugify((row.p_fn || "") + " " + (row.p_ln || "")) + "-" + uid : null;
  const v = { id: rid, uid, file, uslug: row.p_uslug || gen, uslug_gen: gen, cslug_db: row.p_cslug_db || null, cslug_pl: txSlugify(row.p_cname_pl) || null, cslug_name: txSlugify(row.p_cname) || null };
  v.cslug = v.cslug_db || v.cslug_pl || v.cslug_name;
  return v;
}
__name(txPathVars, "txPathVars");
__name2(txPathVars, "txPathVars");
function txPathTemplate(path, v) {
  let p = path;
  try {
    p = decodeURIComponent(path);
  } catch (e) {
  }
  if (p.startsWith("/public/")) p = p.slice("/public".length);
  if (!v.file || p.indexOf(v.file) < 0) return null;
  p = p.split(v.file).join("{file}");
  for (const k of ["uslug", "uslug_gen", "cslug_db", "cslug_pl", "cslug_name", "id", "uid"]) {
    if (v[k] && p.indexOf("/" + v[k] + "/") >= 0) p = p.split("/" + v[k] + "/").join("/{" + k + "}/");
  }
  return p;
}
__name(txPathTemplate, "txPathTemplate");
__name2(txPathTemplate, "txPathTemplate");
function txPathFill(tpl, v) {
  return tpl.replace(/\{(\w+)\}/g, (m, k) => k === "file" ? encodeURIComponent(v.file || "") : encodeURIComponent(String(v[k] || "")));
}
__name(txPathFill, "txPathFill");
__name2(txPathFill, "txPathFill");
async function txPathTpl(env) {
  const r = await env.DB.prepare("SELECT v FROM tx_kv WHERE k = 'path_tpl2'").first();
  return r && r.v || TX_PATH_DEFAULT;
}
__name(txPathTpl, "txPathTpl");
__name2(txPathTpl, "txPathTpl");
var TX_TRAINER_NAME_RX = /(^|[^\p{L}])(tren(er|erka)|trainer|tr[ée]ner(ka)?)([^\p{L}]|$)/iu;
var TX_TRAINER_SET = null;
var TX_TRAINER_SET_AT = 0;
async function txTrainerSet(env) {
  if (TX_TRAINER_SET && Date.now() - TX_TRAINER_SET_AT < 6e4) return TX_TRAINER_SET;
  try {
    const rs = await env.DB.prepare("SELECT ns_user_id FROM trainer_uploader").all();
    TX_TRAINER_SET = new Set((rs.results || []).map((x) => String(x.ns_user_id)));
  } catch (e) {
    TX_TRAINER_SET = /* @__PURE__ */ new Set();
  }
  TX_TRAINER_SET_AT = Date.now();
  return TX_TRAINER_SET;
}
__name(txTrainerSet, "txTrainerSet");
__name2(txTrainerSet, "txTrainerSet");
function txTrainerEmails(env) {
  return String(env.TRAINER_UPLOADER_EMAILS || "jakanie@gmail.com").toLowerCase().split(/[,;\s]+/).filter(Boolean);
}
__name(txTrainerEmails, "txTrainerEmails");
async function txIsTrainerAcc(env, uid, first, last, email) {
  if (TX_TRAINER_NAME_RX.test(((first || "") + " " + (last || "")).trim())) return true;
  if (email && txTrainerEmails(env).indexOf(String(email).toLowerCase()) >= 0) return true;
  return uid ? (await txTrainerSet(env)).has(String(uid)) : false;
}
__name(txIsTrainerAcc, "txIsTrainerAcc");
__name2(txIsTrainerAcc, "txIsTrainerAcc");
async function txTalkStats(env, sid) {
  await ensureTranscriptTables(env);
  const pg = new Client({ connectionString: env.HYPERDRIVE.connectionString });
  await pg.connect();
  let recs = [];
  try {
    recs = (await pg.query(`SELECT r.id::text AS id, COALESCE(r.date, r.created_at::date) AS d, r.review_details, COALESCE(rct.name, rc.name) AS category
      FROM records r LEFT JOIN record_categories rc ON rc.id = r.record_category_id
      LEFT JOIN record_category_translations rct ON rct.record_category_id = rc.id AND rct.locale = 'pl'
      WHERE r.user_id::text = $1 AND r.deleted_at IS NULL AND r.record_file IS NOT NULL ORDER BY r.created_at DESC LIMIT 3000`, [String(sid)])).rows;
  } finally {
    try {
      await pg.end();
    } catch (e) {
    }
  }
  recs = recs.filter((r) => !isNotCountedReview(r.review_details));
  const tx = {};
  for (let i = 0; i < recs.length; i += 90) {
    const chunk = recs.slice(i, i + 90).map((r) => r.id);
    const rs = await env.DB.prepare(`SELECT ns_record_id, dur, words, speakers, spk_src FROM record_transcript WHERE dur > 0 AND ns_record_id IN (${chunk.map(() => "?").join(",")})`).bind(...chunk).all();
    for (const x of rs.results || []) tx[x.ns_record_id] = x;
  }
  const now = /* @__PURE__ */ new Date(), dayMs = 864e5;
  const ymd = /* @__PURE__ */ __name((d) => (d instanceof Date ? d.toISOString() : String(d || "")).slice(0, 10), "ymd");
  const today = ymd(now), d7 = ymd(new Date(now - 6 * dayMs)), d30 = ymd(new Date(now - 29 * dayMs));
  const p7 = ymd(new Date(now - 13 * dayMs)), p30 = ymd(new Date(now - 59 * dayMs));
  const weekKey = /* @__PURE__ */ __name((s) => {
    const d = /* @__PURE__ */ new Date(s + "T12:00:00Z");
    d.setUTCDate(d.getUTCDate() - (d.getUTCDay() + 6) % 7);
    return d.toISOString().slice(0, 10);
  }, "weekKey");
  const z = /* @__PURE__ */ __name(() => ({ min: 0, words: 0, n: 0 }), "z");
  const cats = {}, weeks = {}, tot = { all: z(), d30: z(), d7: z(), today: z(), prev7: z(), prev30: z() };
  let noText = 0;
  for (const r of recs) {
    const t = tx[r.id];
    const d = ymd(r.d);
    if (!t) {
      noText++;
      continue;
    }
    const min = (Number(t.dur) || 0) / 60;
    let words = 0, wc = [];
    try {
      wc = JSON.parse(t.words || "[]");
    } catch (e) {
    }
    let sp = null;
    try {
      sp = t.speakers ? JSON.parse(t.speakers) : null;
    } catch (e) {
    }
    const persons = sp && sp.length === wc.length ? new Set(sp).size : 1;
    if (persons < 2) {
      if (txIsMonolog(r.category)) words = wc.length;
    } else if (t.spk_src === "ai-text") words = sp.filter((x) => x === 0).length;
    const c = cats[r.category || "(bez kategorii)"] = cats[r.category || "(bez kategorii)"] || { category: r.category || "(bez kategorii)", n: 0, min: 0, words: 0, n30: 0, min30: 0, words30: 0, n7: 0, min7: 0, words7: 0 };
    c.n++;
    c.min += min;
    c.words += words;
    if (d >= d30) {
      c.n30++;
      c.min30 += min;
      c.words30 += words;
    }
    if (d >= d7) {
      c.n7++;
      c.min7 += min;
      c.words7 += words;
    }
    const add = /* @__PURE__ */ __name((o) => {
      o.n++;
      o.min += min;
      o.words += words;
    }, "add");
    add(tot.all);
    if (d >= d30) add(tot.d30);
    if (d >= d7) add(tot.d7);
    if (d === today) add(tot.today);
    if (d >= p7 && d < d7) add(tot.prev7);
    if (d >= p30 && d < d30) add(tot.prev30);
    const wk = weekKey(d);
    const w = weeks[wk] = weeks[wk] || { week: wk, min: 0, words: 0, n: 0 };
    add(w);
  }
  const r1 = /* @__PURE__ */ __name((x) => Math.round(x * 10) / 10, "r1");
  const fix = /* @__PURE__ */ __name((o) => ({ n: o.n, min: r1(o.min), words: o.words }), "fix");
  return {
    ok: true,
    totals: { all: fix(tot.all), d30: fix(tot.d30), d7: fix(tot.d7), today: fix(tot.today), prev7: fix(tot.prev7), prev30: fix(tot.prev30) },
    categories: Object.values(cats).map((c) => ({ category: c.category, n: c.n, min: r1(c.min), words: c.words, n30: c.n30, min30: r1(c.min30), words30: c.words30, n7: c.n7, min7: r1(c.min7), words7: c.words7 })).sort((a, b) => b.min - a.min),
    recordings_total: recs.length,
    weeks: Object.values(weeks).sort((a, b) => a.week.localeCompare(b.week)).slice(-12).map((w) => ({ week: w.week, n: w.n, min: r1(w.min), words: w.words })),
    without_text: noText,
    note: "Minuty: nagrania z tekstem. S\u0142owa: monologi z jednym g\u0142osem + s\u0142owa kursanta w rozmowach podzielonych przez AI."
  };
}
__name(txTalkStats, "txTalkStats");
__name2(txTalkStats, "txTalkStats");
async function txRecordInfo(env, rid) {
  if (!rid || !env.HYPERDRIVE) return {};
  const pg = new Client({ connectionString: env.HYPERDRIVE.connectionString });
  try {
    await pg.connect();
    const r = await pg.query(`SELECT u.country, u.id::text AS uid, u.first_name AS fn, u.last_name AS ln, r.record_file, COALESCE(rct.name, rc.name) AS category, ${TX_PATH_SQL_COLS} FROM records r LEFT JOIN users u ON u.id = r.user_id
      LEFT JOIN record_categories rc ON rc.id = r.record_category_id
      LEFT JOIN record_category_translations rct ON rct.record_category_id = rc.id AND rct.locale = 'pl'
      WHERE r.id::text = $1`, [String(rid)]);
    const x = r.rows[0] || {};
    return {
      lang: txLangForCountry(x.country),
      category: x.category || null,
      uid: x.uid || null,
      file: x.record_file || null,
      vars: txPathVars(x, String(rid), x.uid, x.record_file),
      trainerAcc: await txIsTrainerAcc(env, x.uid, x.fn, x.ln, x.p_email),
      studentName: ((x.fn || "") + " " + (x.ln || "")).trim()
    };
  } catch (e) {
    return {};
  } finally {
    try {
      await pg.end();
    } catch (e) {
    }
  }
}
__name(txRecordInfo, "txRecordInfo");
__name2(txRecordInfo, "txRecordInfo");
function mp3Chunks(buf, targetSec) {
  const b = new Uint8Array(buf);
  let i = 0;
  if (b[0] === 73 && b[1] === 68 && b[2] === 51) i = 10 + ((b[6] & 127) << 21 | (b[7] & 127) << 14 | (b[8] & 127) << 7 | b[9] & 127);
  const BR = { 1: [0, 32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320], 2: [0, 8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160] };
  const SR = { 3: [44100, 48e3, 32e3], 2: [22050, 24e3, 16e3], 0: [11025, 12e3, 8e3] };
  const chunks = [];
  let start = i, acc = 0, frames = 0, chunkStartSec = 0, total = 0;
  while (i + 4 <= b.length) {
    if (b[i] !== 255 || (b[i + 1] & 224) !== 224) {
      i++;
      continue;
    }
    const ver = b[i + 1] >> 3 & 3, layer = b[i + 1] >> 1 & 3, bri = b[i + 2] >> 4, sri = b[i + 2] >> 2 & 3, pad = b[i + 2] >> 1 & 1;
    if (ver === 1 || layer !== 1 || bri === 0 || bri === 15 || sri === 3) {
      i++;
      continue;
    }
    const sr = SR[ver][sri], kbps = BR[ver === 3 ? 1 : 2][bri], spf = ver === 3 ? 1152 : 576;
    const len = Math.floor((ver === 3 ? 144 : 72) * kbps * 1e3 / sr) + pad;
    if (len < 4) {
      i++;
      continue;
    }
    acc += spf / sr;
    total += spf / sr;
    frames++;
    i += len;
    if (acc >= targetSec) {
      chunks.push({ bytes: buf.slice(start, i), offset: chunkStartSec, dur: acc });
      chunkStartSec += acc;
      start = i;
      acc = 0;
    }
  }
  if (i > start && acc > 0.3) chunks.push({ bytes: buf.slice(start, Math.min(i, b.length)), offset: chunkStartSec, dur: acc });
  return frames > 10 ? chunks : null;
}
__name(mp3Chunks, "mp3Chunks");
__name2(mp3Chunks, "mp3Chunks");
__name2(transcriptDailyLimit, "transcriptDailyLimit");
function abToBase64(buf) {
  try {
    if (typeof Buffer !== "undefined") return Buffer.from(buf).toString("base64");
  } catch (e) {
  }
  const b = new Uint8Array(buf);
  let s = "";
  for (let i = 0; i < b.length; i += 32768) s += String.fromCharCode.apply(null, b.subarray(i, i + 32768));
  return btoa(s);
}
__name(abToBase64, "abToBase64");
__name2(abToBase64, "abToBase64");
function transcriptOut(r) {
  if (!r) return null;
  return {
    record_id: r.ns_record_id,
    status: r.status,
    text: r.text,
    lang: r.lang,
    dur: r.dur,
    words: r.words ? JSON.parse(r.words) : [],
    speakers: r.speakers ? JSON.parse(r.speakers) : null,
    spk_names: r.spk_names ? JSON.parse(r.spk_names) : null,
    spk_src: r.spk_src || null,
    summary: (() => {
      try {
        return r.summary ? JSON.parse(r.summary) : null;
      } catch (e) {
        return null;
      }
    })(),
    fixed: (() => {
      try {
        return r.fixed ? JSON.parse(r.fixed) : null;
      } catch (e) {
        return null;
      }
    })(),
    src_file: r.src_file || null,
    spk_people: (() => {
      try {
        return r.spk_people ? JSON.parse(r.spk_people) : null;
      } catch (e) {
        return null;
      }
    })(),
    edited_by: r.edited_by || null,
    created_at: r.created_at,
    error: r.error || null
  };
}
__name(transcriptOut, "transcriptOut");
__name2(transcriptOut, "transcriptOut");
var TX_LLM_MODEL = "@cf/meta/llama-3.3-70b-instruct-fp8-fast";
function txAiDailyLimit(env) {
  const v = parseFloat(env.AI_SPEAKERS_DAILY);
  return isFinite(v) && v >= 0 ? v : 2e4;
}
__name(txAiDailyLimit, "txAiDailyLimit");
function txSentences(words) {
  const out = [];
  let cur = null;
  for (let i = 0; i < words.length; i++) {
    if (!cur) {
      cur = { from: i, to: i };
      out.push(cur);
    } else cur.to = i;
    if (/(?<!\.)[.?!]["”»)]*$/.test(String(words[i][0] || "")) || i - cur.from > 60) cur = null;
  }
  return out.map((c) => ({ from: c.from, to: c.to, text: words.slice(c.from, c.to + 1).map((w) => w[0]).join(" ").replace(/\s+/g, " ").trim() }));
}
__name(txSentences, "txSentences");
__name2(txSentences, "txSentences");
function txParseJson(t) {
  if (t && typeof t === "object") return t;
  t = String(t || "");
  const a = t.indexOf("{"), b = t.lastIndexOf("}");
  if (a < 0 || b <= a) return null;
  try {
    return JSON.parse(t.slice(a, b + 1));
  } catch (e) {
    return null;
  }
}
__name(txParseJson, "txParseJson");
__name2(txParseJson, "txParseJson");
var TX_LLM_BEST = "@cf/openai/gpt-oss-120b";
function txLlmText(res) {
  if (!res) return "";
  if (typeof res === "string") return res;
  if (typeof res.response === "string") return res.response;
  if (res.response && typeof res.response === "object") return JSON.stringify(res.response);
  if (typeof res.output_text === "string") return res.output_text;
  if (Array.isArray(res.output)) {
    const parts = [];
    for (const o of res.output) if (o && o.type === "message" && Array.isArray(o.content)) {
      for (const c of o.content) if (c && typeof c.text === "string") parts.push(c.text);
    }
    if (parts.length) return parts.join("\n");
  }
  if (res.choices && res.choices[0] && res.choices[0].message) return String(res.choices[0].message.content || "");
  return "";
}
__name(txLlmText, "txLlmText");
__name2(txLlmText, "txLlmText");
async function txLlm(env, sys, user, maxTokens) {
  let text = "";
  try {
    const r1 = await env.AI.run(TX_LLM_BEST, { input: [{ role: "developer", content: sys }, { role: "user", content: user }], reasoning: { effort: "low" }, max_output_tokens: (maxTokens || 1500) + 6e3 });
    text = txLlmText(r1);
  } catch (e) {
    console.log("gpt-oss", String(e && e.message || e).slice(0, 200));
  }
  if (!text) {
    const inp = { messages: [{ role: "system", content: sys }, { role: "user", content: user }], max_tokens: maxTokens || 1500, temperature: 0.2 };
    let res;
    try {
      res = await env.AI.run(TX_LLM_MODEL, inp);
    } catch (e) {
      res = await env.AI.run("@cf/meta/llama-3.1-8b-instruct", inp);
    }
    text = txLlmText(res);
  }
  const day = (/* @__PURE__ */ new Date()).toISOString().slice(0, 10);
  try {
    await env.DB.prepare("INSERT INTO llm_usage (day, calls) VALUES (?, 1) ON CONFLICT(day) DO UPDATE SET calls = calls + 1").bind(day).run();
  } catch (e) {
  }
  return text;
}
__name(txLlm, "txLlm");
__name2(txLlm, "txLlm");
async function txAiSpeakers(env, words, category, lang, force, trainerAcc) {
  if (!env.AI) return { speakers: null, src: "ai-err", note: "brak Workers AI" };
  if (txIsMonolog(category) && !force) return { speakers: null, src: "ai-one", note: "monolog" };
  const sents = txSentences(words);
  if (sents.length < 2) return { speakers: null, src: "ai-one", note: "jedno zdanie" };
  const day = (/* @__PURE__ */ new Date()).toISOString().slice(0, 10);
  const u = await env.DB.prepare("SELECT calls FROM llm_usage WHERE day = ?").bind(day).first();
  if (u && u.calls >= txAiDailyLimit(env)) return { speakers: null, src: null, note: "dzienny limit AI wyczerpany" };
  const who = trainerAcc ? "Nagrywa TRENER programu Nowa Mowa \u2014 to jego rozmowa z kursantem (cz\u0119sto przez telefon). Trener prowadzi, dopytuje, chwali, daje wskaz\xF3wki o systemach mowy (zer\xF3wka, \u0142\u0105cznik, jedynka\u2026). Osoba 1 = trener, osoba 2 = kursant." : "Nagrywa kursant programu Nowa Mowa, kt\xF3ry \u0107wiczy codzienne rozmowy (sklep, telefon, urz\u0105d, ulica, praca). Zwykle zaczyna rozmow\u0119: wita si\u0119, pyta, prosi. Osoba 1 = kursant, osoba 2 = rozm\xF3wca (np. sprzedawczyni).";
  const sys = "Dzielisz zapis rozmowy (z rozpoznawania mowy, mog\u0105 by\u0107 b\u0142\u0119dy) na wypowiedzi os\xF3b. " + who + ' ZASADY: jedna osoba zwykle m\xF3wi kilka zda\u0144 z rz\u0119du \u2014 zmieniaj osob\u0119 tylko, gdy wynika to z sensu (pytanie \u2192 odpowied\u017A, reakcja na to, co powiedzia\u0142 rozm\xF3wca, kr\xF3tkie \u201EMhm\u201D, \u201ETak\u201D, \u201ENo\u201D, \u201EO, widzisz\u201D wtr\u0105cone w opowie\u015B\u0107 drugiej osoby, po\u017Cegnanie i odpowied\u017A na nie). Opowie\u015B\u0107 w 1. osobie (\u201Eja\u201D, \u201Eby\u0142am\u201D) ci\u0105gnie si\u0119 u tej samej osoby. Zwykle s\u0105 DOK\u0141ADNIE 2 osoby. Osoba 3 tylko wtedy, gdy wyra\u017Anie odzywa si\u0119 kto\u015B trzeci (np. kto\u015B w tle zwraca si\u0119 do rozm\xF3wc\xF3w) \u2014 nie u\u017Cywaj jej do \u201Eniepewnych\u201D zda\u0144. Odpowiedz WY\u0141\u0104CZNIE JSON-em: {"roles":{"1":"' + (trainerAcc ? "trener" : "kursant") + '","2":"kr\xF3tka rola i imi\u0119, je\u015Bli pada"},"zmiany":[[numer_zdania, osoba], \u2026]} \u2014 lista miejsc, w kt\xF3rych zaczyna m\xF3wi\u0107 kolejna osoba (pierwszy element dotyczy zdania 1).';
  const BATCH = 120, labels = [], roles = { "1": trainerAcc ? "trener" : "kursant" };
  for (let i = 0; i < sents.length; i += BATCH) {
    const part = sents.slice(i, i + BATCH);
    const prevN = Math.min(8, i);
    const ctx = prevN ? "Koniec poprzedniego fragmentu (ju\u017C przypisane):\n" + sents.slice(i - prevN, i).map((x, k) => "[osoba " + labels[i - prevN + k] + "] " + x.text).join("\n") + "\n\n" : "";
    const user = "Kategoria nagrania: " + (category || "\u2014") + ". J\u0119zyk: " + (lang || "?") + ".\n" + ctx + "Zdania (" + part.length + "):\n" + part.map((x, k) => k + 1 + ". " + x.text).join("\n");
    const pickChanges = /* @__PURE__ */ __name((j2) => {
      if (!j2) return null;
      const a = j2.zmiany || j2.changes || j2.turns || j2.zmiana;
      if (Array.isArray(a) && a.length) return a;
      if (Array.isArray(j2.s) && j2.s.length) return j2.s.map((x, k) => [k + 1, x]);
      return null;
    }, "pickChanges");
    let txt = "", j = null, ch = null;
    try {
      txt = await txLlm(env, sys, user, 300 + part.length * 3);
      j = txParseJson(txt);
      ch = pickChanges(j);
    } catch (e) {
      txt = String(e && e.message || e);
    }
    if (!ch) {
      try {
        const res = await env.AI.run(TX_LLM_MODEL, { messages: [{ role: "system", content: sys }, { role: "user", content: user + "\n\nOdpowiedz tylko JSON-em z polami roles i zmiany." }], max_tokens: 400 + part.length * 4, temperature: 0 });
        j = txParseJson(txLlmText(res));
        ch = pickChanges(j);
      } catch (e) {
      }
    }
    if (!ch || !ch.length) return { speakers: null, src: "ai-err", note: "AI: nieczytelna odpowied\u017A (" + String(txt || "").replace(/\s+/g, " ").slice(0, 80) + ")" };
    const arr = new Array(part.length).fill(0);
    ch = ch.map((c) => Array.isArray(c) ? [parseInt(c[0], 10), parseInt(c[1], 10)] : [parseInt(c.zdanie || c.nr, 10), parseInt(c.osoba, 10)]).filter((c) => c[0] >= 1 && c[0] <= part.length && c[1] >= 1 && c[1] <= 3).sort((a, b) => a[0] - b[0]);
    let cur = ch.length && ch[0][0] === 1 ? ch[0][1] : labels.length ? labels[labels.length - 1] : 1, ci = 0;
    for (let k = 0; k < part.length; k++) {
      while (ci < ch.length && ch[ci][0] === k + 1) {
        cur = ch[ci][1];
        ci++;
      }
      arr[k] = cur;
    }
    labels.push(...arr);
    if (j.roles && typeof j.roles === "object") {
      for (const k of ["1", "2", "3"]) if (j.roles[k] && (k !== "1" ? true : !roles[k])) roles[k] = String(j.roles[k]).slice(0, 40);
    }
  }
  const n3 = labels.filter((x) => x === 3).length;
  if (n3 && n3 < labels.length * 0.05) {
    for (let k = 0; k < labels.length; k++) if (labels[k] === 3) labels[k] = 2;
  }
  const speakers = new Array(words.length).fill(0);
  sents.forEach((x, k) => {
    for (let w = x.from; w <= x.to; w++) speakers[w] = labels[k] - 1;
  });
  const n = new Set(speakers).size;
  if (n < 2) return { speakers: null, src: "ai-one", note: "AI: jedna osoba" };
  const names = {};
  for (const k of [...new Set(speakers)]) names[k] = roles[String(k + 1)] || (k === 0 ? trainerAcc ? "trener" : "kursant" : trainerAcc && k === 1 ? "kursant" : "rozm\xF3wca " + k);
  return { speakers, names, src: "ai-text", note: "AI z tre\u015Bci: osoby " + n };
}
__name(txAiSpeakers, "txAiSpeakers");
__name2(txAiSpeakers, "txAiSpeakers");
async function txDiarize(env, rid, audioBuf, lang, words, category, dur) {
  const day = (/* @__PURE__ */ new Date()).toISOString().slice(0, 10);
  if (!diarEnabled(env)) return { speakers: null, note: null };
  if (txIsMonolog(category)) return { speakers: null, note: "monolog \u2014 bez rozpoznawania g\u0142os\xF3w" };
  return await txDiarizeAudio(env, rid, audioBuf, lang, words, category, dur, day);
}
__name(txDiarize, "txDiarize");
__name2(txDiarize, "txDiarize");
async function txDiarizeAudio(env, rid, audioBuf, lang, words, category, dur, day) {
  if (words.length <= 3) return { speakers: null, note: "za ma\u0142o s\u0142\xF3w" };
  let speakers = null, note = null;
  try {
    const du = await env.DB.prepare("SELECT minutes FROM diar_usage WHERE day = ?").bind(day).first();
    if (du && du.minutes >= diarDailyLimit(env)) return { speakers: null, note: "dzienny limit rozpoznawania g\u0142os\xF3w wyczerpany" };
    const d = await diarizeWords(env, audioBuf, lang, words);
    speakers = d.speakers;
    note = d.note;
    await env.DB.prepare("INSERT INTO diar_usage (day, minutes) VALUES (?, ?) ON CONFLICT(day) DO UPDATE SET minutes = minutes + excluded.minutes").bind(day, (d.dur || dur || 0) / 60).run();
  } catch (e) {
    note = "B\u0141\u0104D: " + String(e && e.message || e).slice(0, 300);
    console.log("diarize", rid, note);
  }
  try {
    await env.DB.prepare("INSERT INTO tx_kv (k, v) VALUES ('diar_last', ?) ON CONFLICT(k) DO UPDATE SET v = excluded.v").bind((/* @__PURE__ */ new Date()).toISOString().slice(0, 16) + " " + note).run();
  } catch (e) {
  }
  return { speakers, note, src: speakers ? "deepgram" : null };
}
__name(txDiarizeAudio, "txDiarizeAudio");
__name2(txDiarizeAudio, "txDiarizeAudio");
async function transcribeAndStore(env, rid, sid, audioBuf, forcedLang, category, trainerAcc, srcFile) {
  await ensureTranscriptTables(env);
  const now = Math.floor(Date.now() / 1e3);
  const day = (/* @__PURE__ */ new Date()).toISOString().slice(0, 10);
  const used = await env.DB.prepare("SELECT minutes FROM transcript_usage WHERE day = ?").bind(day).first();
  if (used && used.minutes >= transcriptDailyLimit(env)) return { ns_record_id: rid, status: "limit", error: "Dzienny limit przepisywania wyczerpany \u2014 reszta przepisze si\u0119 jutro" };
  if (!env.AI) return { ns_record_id: rid, status: "error", error: "Brak powi\u0105zania Workers AI (AI) w workerze" };
  let status = "ok", err = null, lang = forcedLang || null, dur = 0;
  const words = [], texts = [], prevTexts = [];
  const parts = mp3Chunks(audioBuf, 30) || [{ bytes: audioBuf, offset: 0, dur: 0 }];
  try {
    for (const part of parts) {
      const inp = { audio: abToBase64(part.bytes), vad_filter: true };
      if (lang) inp.language = lang;
      if (lang && TX_PROMPT[lang]) inp.initial_prompt = TX_PROMPT[lang];
      let res;
      try {
        res = await env.AI.run(WHISPER_MODEL, inp);
      } catch (e1) {
        res = await env.AI.run(WHISPER_MODEL, inp);
      }
      const info2 = res && res.transcription_info || {};
      if (!lang && info2.language) lang = info2.language;
      const segs = res && res.segments || [];
      if (!segs.length) {
        const t = String(res && res.text || "").trim();
        if (t) texts.push(t);
      }
      for (const seg of segs) {
        if (!txSegmentOk(seg, prevTexts)) continue;
        texts.push(String(seg.text || "").trim());
        for (const w of seg.words || []) words.push([String(w.word || "").trim(), Math.round(((w.start || 0) + part.offset) * 100) / 100, Math.round(((w.end || 0) + part.offset) * 100) / 100]);
      }
      dur += part.dur || Number(info2.duration) || 0;
    }
  } catch (e) {
    status = "error";
    err = String(e && e.message || e).slice(0, 300);
  }
  const info = { language: lang, duration: dur };
  const text = texts.join(" ").replace(/\s+/g, " ").trim();
  let speakers = null, diarNote = null, spkNames = null, spkSrc = null;
  if (status === "ok") {
    const dz = await txDiarize(env, rid, audioBuf, lang, words, category, dur);
    speakers = dz.speakers;
    diarNote = dz.note;
    spkSrc = dz.src || null;
    if (!speakers && (!txIsMonolog(category) || trainerAcc) && words.length >= 6) {
      const ai = await txAiSpeakers(env, words, category, lang, !!trainerAcc, trainerAcc);
      speakers = ai.speakers;
      spkNames = ai.names || null;
      spkSrc = ai.src;
      diarNote = (diarNote ? diarNote + " \xB7 " : "") + ai.note;
    }
  }
  await env.DB.prepare(`INSERT INTO record_transcript (ns_record_id, ns_student_id, status, text, lang, dur, words, speakers, diar, spk_names, spk_src, model, error, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    ON CONFLICT(ns_record_id) DO UPDATE SET status=excluded.status, text=excluded.text, lang=excluded.lang, dur=excluded.dur, words=excluded.words, speakers=excluded.speakers, diar=excluded.diar, spk_names=excluded.spk_names, spk_src=excluded.spk_src, model=excluded.model, error=excluded.error, created_at=excluded.created_at`).bind(rid, sid ? String(sid) : null, status, text, info.language || null, dur, JSON.stringify(words), speakers ? JSON.stringify(speakers) : null, diarNote, spkNames ? JSON.stringify(spkNames) : null, spkSrc, WHISPER_MODEL + "|" + TX_VERSION, err, now).run();
  if (status === "ok") {
    await env.DB.prepare("UPDATE record_transcript SET src_file = ?, fixed = NULL, summary = NULL, edited_by = NULL, spk_people = NULL WHERE ns_record_id = ?").bind(srcFile || null, rid).run();
  }
  if (status === "ok" && dur) await env.DB.prepare("INSERT INTO transcript_usage (day, minutes) VALUES (?, ?) ON CONFLICT(day) DO UPDATE SET minutes = minutes + excluded.minutes").bind(day, dur / 60).run();
  return { ns_record_id: rid, ns_student_id: sid, status, text, lang: info.language || null, dur, words: JSON.stringify(words), speakers: speakers ? JSON.stringify(speakers) : null, spk_names: spkNames ? JSON.stringify(spkNames) : null, spk_src: spkSrc, created_at: now, error: err, diar: diarNote };
}
__name(transcribeAndStore, "transcribeAndStore");
__name2(transcribeAndStore, "transcribeAndStore");
async function transcribeTick(env) {
  if (!env.AI || !env.HYPERDRIVE) return;
  await ensureTranscriptTables(env);
  const t0 = Date.now(), day = (/* @__PURE__ */ new Date()).toISOString().slice(0, 10);
  const pg = new Client({ connectionString: env.HYPERDRIVE.connectionString });
  await pg.connect();
  let rows = [];
  try {
    rows = (await pg.query(
      `SELECT r.id::text AS id, r.user_id::text AS user_id, r.record_file, u.country, COALESCE(rct.name, rc.name) AS category, ${TX_PATH_SQL_COLS} FROM records r
       LEFT JOIN users u ON u.id = r.user_id
       LEFT JOIN record_categories rc ON rc.id = r.record_category_id
       LEFT JOIN record_category_translations rct ON rct.record_category_id = rc.id AND rct.locale = 'pl'
       WHERE r.deleted_at IS NULL AND r.record_file IS NOT NULL AND r.created_at > now() - interval '90 days'
       ORDER BY r.created_at DESC LIMIT 8000`
    )).rows;
  } finally {
    try {
      await pg.end();
    } catch (e) {
    }
  }
  if (!rows.length) return;
  const state = {};
  for (let i = 0; i < rows.length; i += 90) {
    const chunk = rows.slice(i, i + 90).map((r) => r.id);
    const rs = await env.DB.prepare(`SELECT ns_record_id, status, model, created_at, (speakers IS NOT NULL) AS has_sp, diar, spk_src, (summary IS NOT NULL) AS has_sum, (fixed IS NOT NULL) AS has_fix, src_file FROM record_transcript WHERE ns_record_id IN (${chunk.map(() => "?").join(",")})`).bind(...chunk).all();
    for (const x of rs.results || []) state[x.ns_record_id] = x;
  }
  const nowS = Math.floor(Date.now() / 1e3);
  const changed = rows.filter((r) => {
    const x = state[r.id];
    return x && x.status === "ok" && x.src_file && r.record_file && x.src_file !== r.record_file;
  });
  const noSrc = rows.filter((r) => {
    const x = state[r.id];
    return x && x.status === "ok" && !x.src_file && r.record_file;
  }).slice(0, 300);
  for (let i = 0; i < noSrc.length; i += 50) {
    await env.DB.batch(noSrc.slice(i, i + 50).map((r) => env.DB.prepare("UPDATE record_transcript SET src_file = ? WHERE ns_record_id = ? AND src_file IS NULL").bind(r.record_file, r.id)));
  }
  const missing = changed.concat(rows.filter((r) => !state[r.id] || state[r.id].status === "error" && nowS - state[r.id].created_at > 6 * 3600));
  const outdated = rows.filter((r) => state[r.id] && state[r.id].status === "ok" && !/\|v[345]-/.test(String(state[r.id].model || "")));
  const tset = await txTrainerSet(env);
  const trMails = txTrainerEmails(env);
  const isTr = /* @__PURE__ */ __name((r) => TX_TRAINER_NAME_RX.test(((r.p_fn || "") + " " + (r.p_ln || "")).trim()) || tset.has(String(r.user_id)) || trMails.indexOf(String(r.p_email || "").toLowerCase()) >= 0, "isTr");
  const aiQueue = rows.filter((r) => {
    const x = state[r.id];
    return x && x.status === "ok" && !x.has_sp && !x.spk_src && (!txIsMonolog(r.category) || isTr(r));
  }).slice(0, 15);
  for (const r of aiQueue) {
    if (Date.now() - t0 > 2e4) break;
    const t = await env.DB.prepare("SELECT words, lang FROM record_transcript WHERE ns_record_id = ?").bind(r.id).first();
    let words = [];
    try {
      words = JSON.parse(t && t.words || "[]");
    } catch (e) {
    }
    if (words.length < 6) {
      await env.DB.prepare("UPDATE record_transcript SET spk_src = 'ai-one' WHERE ns_record_id = ?").bind(r.id).run();
      continue;
    }
    const ai = await txAiSpeakers(env, words, r.category, t && t.lang, txIsMonolog(r.category), await txIsTrainerAcc(env, r.user_id, r.p_fn, r.p_ln, r.p_email));
    if (ai.src === null) break;
    await env.DB.prepare("UPDATE record_transcript SET speakers = ?, spk_names = ?, spk_src = ?, diar = ? WHERE ns_record_id = ?").bind(ai.speakers ? JSON.stringify(ai.speakers) : null, ai.names ? JSON.stringify(ai.names) : null, ai.src, ai.note, r.id).run();
  }
  const autoSum = rows.filter((r) => {
    const x = state[r.id];
    return x && x.status === "ok" && x.spk_src && !x.has_sum && isTr(r);
  }).slice(0, 1);
  for (const r of autoSum) {
    if (Date.now() - t0 > 25e3) break;
    try {
      let t = await env.DB.prepare("SELECT * FROM record_transcript WHERE ns_record_id = ?").bind(r.id).first();
      const ri = { lang: txLangForCountry(r.country), category: r.category, trainerAcc: true, studentName: ((r.p_fn || "") + " " + (r.p_ln || "")).trim() };
      if (!t.fixed) {
        const f = await txFixText(env, t, r.category);
        if (f) {
          await env.DB.prepare("UPDATE record_transcript SET fixed = ? WHERE ns_record_id = ?").bind(JSON.stringify(f), r.id).run();
          t.fixed = JSON.stringify(f);
        }
      }
      const sm = await txMakeSummary(env, r.id, t, ri);
      await env.DB.prepare("UPDATE record_transcript SET summary = ? WHERE ns_record_id = ?").bind(JSON.stringify(sm.summary || { error: sm.error, created_at: Math.floor(Date.now() / 1e3) }), r.id).run();
    } catch (e) {
      console.log("autoSum", r.id, String(e && e.message || e).slice(0, 200));
    }
  }
  const diarRetry = !diarEnabled(env) ? [] : rows.filter((r) => {
    const x = state[r.id];
    return x && x.status === "ok" && String(x.model || "").endsWith(TX_VERSION) && !x.has_sp && !x.spk_src && !txIsMonolog(r.category) && (!x.diar || /^BŁĄD/.test(x.diar));
  }).slice(0, 4);
  for (const r of diarRetry) r._diarOnly = true;
  const queue = diarRetry.concat(missing, outdated);
  const pathTpl = await txPathTpl(env);
  const ae0 = await env.DB.prepare("SELECT v FROM tx_kv WHERE k = 'ai_err_retry'").first();
  if (!ae0 || ae0.v !== day) {
    await env.DB.prepare("UPDATE record_transcript SET spk_src = NULL WHERE spk_src = 'ai-err'").run();
    await env.DB.prepare("INSERT INTO tx_kv (k, v) VALUES ('ai_err_retry', ?) ON CONFLICT(k) DO UPDATE SET v = excluded.v").bind(day).run();
  }
  const ar0 = await env.DB.prepare("SELECT v FROM tx_kv WHERE k = 'ai_spk_v'").first();
  if (!ar0 || ar0.v !== "2") {
    await env.DB.prepare("UPDATE record_transcript SET speakers = NULL, spk_names = NULL, spk_src = NULL WHERE spk_src IN ('ai-text', 'ai-one', 'ai-err')").run();
    await env.DB.prepare("INSERT INTO tx_kv (k, v) VALUES ('ai_spk_v', '2') ON CONFLICT(k) DO UPDATE SET v = excluded.v").run();
  }
  const dr0 = await env.DB.prepare("SELECT v FROM tx_kv WHERE k = 'diar_reset'").first();
  if (!dr0 || dr0.v !== "1") {
    await env.DB.prepare("UPDATE record_transcript SET diar = NULL WHERE speakers IS NULL AND diar LIKE 'Nova: brak%'").run();
    await env.DB.prepare("INSERT INTO tx_kv (k, v) VALUES ('diar_reset', '1') ON CONFLICT(k) DO UPDATE SET v = excluded.v").run();
  }
  const rs0 = await env.DB.prepare("SELECT v FROM tx_kv WHERE k = 'path_reset'").first();
  if (!rs0 || rs0.v !== "3") {
    await env.DB.prepare("DELETE FROM record_transcript WHERE status = 'error' AND error LIKE 'Nie uda\u0142o si\u0119 pobra\u0107 pliku%'").run();
    await env.DB.prepare("INSERT INTO tx_kv (k, v) VALUES ('path_reset', '3') ON CONFLICT(k) DO UPDATE SET v = excluded.v").run();
  }
  for (const next of queue) {
    if (Date.now() - t0 > 4e4) break;
    const used = await env.DB.prepare("SELECT minutes FROM transcript_usage WHERE day = ?").bind(day).first();
    if (used && used.minutes >= transcriptDailyLimit(env)) break;
    const path = txPathFill(pathTpl, txPathVars(next, next.id, next.user_id, next.record_file));
    const a = await fetch(nsServerBase("new") + path);
    if (!a.ok) {
      await env.DB.prepare("INSERT INTO record_transcript (ns_record_id, ns_student_id, status, error, created_at) VALUES (?, ?, 'error', ?, ?) ON CONFLICT(ns_record_id) DO UPDATE SET status = 'error', error = excluded.error, created_at = excluded.created_at").bind(next.id, next.user_id, "Nie uda\u0142o si\u0119 pobra\u0107 pliku (" + a.status + ") \u2014 " + path, nowS).run();
      continue;
    }
    if (next._diarOnly) {
      const t = await env.DB.prepare("SELECT words, lang, dur FROM record_transcript WHERE ns_record_id = ?").bind(next.id).first();
      let words = [];
      try {
        words = JSON.parse(t && t.words || "[]");
      } catch (e) {
      }
      const dz = await txDiarize(env, next.id, await a.arrayBuffer(), t && t.lang || txLangForCountry(next.country), words, next.category, t && t.dur);
      await env.DB.prepare("UPDATE record_transcript SET speakers = ?, diar = ? WHERE ns_record_id = ?").bind(dz.speakers ? JSON.stringify(dz.speakers) : null, dz.note, next.id).run();
      continue;
    }
    await transcribeAndStore(env, next.id, next.user_id, await a.arrayBuffer(), txLangForCountry(next.country), next.category, await txIsTrainerAcc(env, next.user_id, next.p_fn, next.p_ln, next.p_email), next.record_file);
  }
}
__name(transcribeTick, "transcribeTick");
__name2(transcribeTick, "transcribeTick");
async function handleTranscriptsGet(request, env, url) {
  const who = await contactAuth(env, url.searchParams.get("ns_token"), url.searchParams.get("ns_email"), url.searchParams.get("ns_server"));
  if (!who) return json({ error: "Nieprawid\u0142owy lub wygas\u0142y token NS \u2014 zaloguj si\u0119 ponownie" }, 401);
  if (who.forbidden) return json({ error: "Tylko trener/admin" }, 403);
  await ensureTranscriptTables(env);
  const ids = String(url.searchParams.get("record_ids") || "").split(",").map((x) => x.trim()).filter(Boolean).slice(0, 400);
  const items = {};
  for (let i = 0; i < ids.length; i += 90) {
    const chunk = ids.slice(i, i + 90);
    const rs = await env.DB.prepare(`SELECT * FROM record_transcript WHERE ns_record_id IN (${chunk.map(() => "?").join(",")})`).bind(...chunk).all();
    for (const r of rs.results || []) items[r.ns_record_id] = transcriptOut(r);
  }
  return json({ ok: true, items });
}
__name(handleTranscriptsGet, "handleTranscriptsGet");
__name2(handleTranscriptsGet, "handleTranscriptsGet");
async function handleTranscriptsDayMonolog(request, env, url) {
  const who = await contactAuth(env, url.searchParams.get("ns_token"), url.searchParams.get("ns_email"), url.searchParams.get("ns_server"));
  if (!who) return json({ error: "Nieprawid\u0142owy lub wygas\u0142y token NS \u2014 zaloguj si\u0119 ponownie" }, 401);
  if (who.forbidden) return json({ error: "Tylko trener/admin" }, 403);
  const sid = String(url.searchParams.get("student_id") || ""), date = String(url.searchParams.get("date") || "");
  if (!sid || !/^\d{4}-\d{2}-\d{2}$/.test(date)) return json({ error: "Brak student_id albo daty" }, 400);
  await ensureTranscriptTables(env);
  const pg = new Client({ connectionString: env.HYPERDRIVE.connectionString });
  await pg.connect();
  let rows = [];
  try {
    rows = (await pg.query(`SELECT r.id::text AS id, r.record_file, r.created_at, r.reviewed_at, COALESCE(rct.name, rc.name) AS category, ${TX_PATH_SQL_COLS} FROM records r
      LEFT JOIN users u ON u.id = r.user_id
      LEFT JOIN record_categories rc ON rc.id = r.record_category_id
      LEFT JOIN record_category_translations rct ON rct.record_category_id = rc.id AND rct.locale = 'pl'
      WHERE r.deleted_at IS NULL AND r.record_file IS NOT NULL AND r.user_id::text = $1
        AND COALESCE(r.date, r.created_at::date) = $2::date
      ORDER BY r.created_at`, [sid, date])).rows.filter((r) => txIsMonolog(r.category));
  } finally {
    try {
      await pg.end();
    } catch (e) {
    }
  }
  const items = [], tpl = await txPathTpl(env);
  for (const r of rows) {
    const t = await env.DB.prepare("SELECT * FROM record_transcript WHERE ns_record_id = ?").bind(r.id).first();
    items.push({ record_id: r.id, category: r.category, created_at: r.created_at, reviewed: !!r.reviewed_at, path: txPathFill(tpl, txPathVars(r, r.id, sid, r.record_file)), transcript: t && t.status === "ok" ? transcriptOut(t) : null });
  }
  return json({ ok: true, items });
}
__name(handleTranscriptsDayMonolog, "handleTranscriptsDayMonolog");
__name2(handleTranscriptsDayMonolog, "handleTranscriptsDayMonolog");
async function handleTranscriptsStatus(request, env, url) {
  const who = await contactAuth(env, url.searchParams.get("ns_token"), url.searchParams.get("ns_email"), url.searchParams.get("ns_server"));
  if (!who) return json({ error: "Nieprawid\u0142owy lub wygas\u0142y token NS \u2014 zaloguj si\u0119 ponownie" }, 401);
  if (who.forbidden) return json({ error: "Tylko trener/admin" }, 403);
  await ensureTranscriptTables(env);
  const pg = new Client({ connectionString: env.HYPERDRIVE.connectionString });
  await pg.connect();
  let rows = [];
  try {
    rows = (await pg.query(`SELECT r.id::text AS id, (r.created_at > now() - interval '30 days') AS m30 FROM records r
      WHERE r.deleted_at IS NULL AND r.record_file IS NOT NULL AND r.created_at > now() - interval '90 days'`)).rows;
  } finally {
    try {
      await pg.end();
    } catch (e) {
    }
  }
  const st = {};
  for (let i = 0; i < rows.length; i += 90) {
    const chunk = rows.slice(i, i + 90).map((r) => r.id);
    const rs = await env.DB.prepare(`SELECT ns_record_id, status, model, error FROM record_transcript WHERE ns_record_id IN (${chunk.map(() => "?").join(",")})`).bind(...chunk).all();
    for (const x of rs.results || []) st[x.ns_record_id] = x;
  }
  const mk = /* @__PURE__ */ __name(() => ({ total: 0, done: 0, old_method: 0, errors: 0, waiting: 0 }), "mk");
  const out = { d30: mk(), d90: mk() }, errs = {}, errSample = [];
  for (const r of rows) {
    const x = st[r.id];
    const k = !x ? "waiting" : x.status === "ok" ? String(x.model || "").endsWith(TX_VERSION) ? "done" : "old_method" : x.status === "error" ? "errors" : "waiting";
    for (const b of r.m30 ? [out.d30, out.d90] : [out.d90]) {
      b.total++;
      b[k]++;
    }
    if (k === "errors") {
      const full = String(x.error || "?"), e = full.split(" \u2014 ")[0].slice(0, 80);
      errs[e] = (errs[e] || 0) + 1;
      if (full.indexOf(" \u2014 ") > 0 && errSample.length < 3) errSample.push(full.split(" \u2014 ").slice(1).join(" \u2014 "));
    }
  }
  const day = (/* @__PURE__ */ new Date()).toISOString().slice(0, 10);
  const used = await env.DB.prepare("SELECT minutes FROM transcript_usage WHERE day = ?").bind(day).first();
  const du = await env.DB.prepare("SELECT minutes FROM diar_usage WHERE day = ?").bind(day).first();
  return json({
    ok: true,
    ...out,
    today_minutes: Math.round(used && used.minutes || 0),
    daily_limit: transcriptDailyLimit(env),
    path_template: await txPathTpl(env),
    diar_today_minutes: Math.round(du && du.minutes || 0),
    diar_daily_limit: diarDailyLimit(env),
    errors_by_reason: errs,
    error_paths: errSample,
    diar_last: (await env.DB.prepare("SELECT v FROM tx_kv WHERE k = 'diar_last'").first() || {}).v || null,
    with_voices: (await env.DB.prepare("SELECT COUNT(*) AS n FROM record_transcript WHERE speakers IS NOT NULL").first() || {}).n || 0,
    version: TX_VERSION
  });
}
__name(handleTranscriptsStatus, "handleTranscriptsStatus");
__name2(handleTranscriptsStatus, "handleTranscriptsStatus");
async function handleTranscriptsCorpus(request, env, url) {
  const who = await contactAuth(env, url.searchParams.get("ns_token"), url.searchParams.get("ns_email"), url.searchParams.get("ns_server"));
  if (!who) return json({ error: "Nieprawid\u0142owy lub wygas\u0142y token NS \u2014 zaloguj si\u0119 ponownie" }, 401);
  if (who.forbidden) return json({ error: "Tylko trener/admin" }, 403);
  await ensureTranscriptTables(env);
  const from = url.searchParams.get("from"), to = url.searchParams.get("to");
  const meta = {};
  const pg = new Client({ connectionString: env.HYPERDRIVE.connectionString });
  await pg.connect();
  try {
    const q = await pg.query(
      `SELECT r.id::text AS id, r.date, r.created_at, r.record_file, r.is_correct, r.reviewed_at, r.review_details,
              u.id::text AS uid, u.first_name, u.last_name, u.country AS country, COALESCE(rct.name, rc.name) AS category, ${TX_PATH_SQL_COLS}
       FROM records r JOIN users u ON u.id = r.user_id
       LEFT JOIN record_categories rc ON rc.id = r.record_category_id
       LEFT JOIN record_category_translations rct ON rct.record_category_id = rc.id AND rct.locale = 'pl'
       WHERE r.deleted_at IS NULL AND r.record_file IS NOT NULL
         AND COALESCE(r.date, r.created_at::date) >= COALESCE($1::date, '2000-01-01'::date)
         AND COALESCE(r.date, r.created_at::date) <= COALESCE($2::date, '2100-01-01'::date)
         AND ($4::text IS NULL OR r.user_id::text = $4::text)
         AND ($3::text IS NULL OR r.user_id IN (SELECT s.id FROM users s
               JOIN students_groups_users sgu ON sgu.students_group_id = s.students_group_id
               JOIN users t ON t.id = sgu.user_id WHERE LOWER(t.email) = LOWER($3::text)))
       ORDER BY r.created_at DESC LIMIT 3000`,
      [from || null, to || null, who.admin ? null : String(who.email || "-"), url.searchParams.get("student_id") || null]
    );
    for (const m of q.rows) meta[m.id] = m;
  } finally {
    try {
      await pg.end();
    } catch (e) {
    }
  }
  const ids = Object.keys(meta), rows = [];
  for (let i = 0; i < ids.length; i += 90) {
    const chunk = ids.slice(i, i + 90);
    const rs = await env.DB.prepare(`SELECT ns_record_id, ns_student_id, text, lang, dur, words, speakers, spk_names, spk_src, spk_people, edited_by, summary, fixed, created_at FROM record_transcript WHERE status = 'ok' AND ns_record_id IN (${chunk.map(() => "?").join(",")})`).bind(...chunk).all();
    for (const r of rs.results || []) rows.push(r);
  }
  const tpl = await txPathTpl(env);
  rows.sort((a, b) => String((meta[b.ns_record_id] || {}).created_at || "").localeCompare(String((meta[a.ns_record_id] || {}).created_at || "")));
  const items = [];
  for (const r of rows) {
    const m = meta[r.ns_record_id];
    if (!m) continue;
    const ymd = m.date instanceof Date ? m.date.toISOString().slice(0, 10) : String(m.date || "").slice(0, 10);
    if (from && ymd && ymd < from) continue;
    if (to && ymd && ymd > to) continue;
    let words = [];
    try {
      words = JSON.parse(r.words || "[]");
    } catch (e) {
    }
    let pauses = 0, longest = 0;
    for (let i = 1; i < words.length; i++) {
      const g = words[i][1] - words[i - 1][2];
      if (g >= 1) {
        pauses++;
        if (g > longest) longest = g;
      }
    }
    const span = words.length ? Math.max(0.1, words[words.length - 1][2] - words[0][1]) : r.dur || 0;
    let turns = null, names = null;
    try {
      const sp = r.speakers ? JSON.parse(r.speakers) : null;
      if (sp && sp.length === words.length) {
        turns = [];
        for (let i = 0; i < words.length; i++) {
          const last = turns[turns.length - 1];
          if (last && last[0] === sp[i]) last[1] += " " + words[i][0];
          else turns.push([sp[i], words[i][0]]);
        }
        names = r.spk_names ? JSON.parse(r.spk_names) : null;
      }
    } catch (e) {
      turns = null;
    }
    let summary = null, fixed = null;
    try {
      summary = r.summary ? JSON.parse(r.summary) : null;
    } catch (e) {
    }
    try {
      fixed = r.fixed ? JSON.parse(r.fixed) : null;
    } catch (e) {
    }
    if (summary && summary.error) summary = null;
    let people = null;
    try {
      people = r.spk_people ? JSON.parse(r.spk_people) : null;
    } catch (e) {
    }
    if (!names && r.spk_names) {
      try {
        names = JSON.parse(r.spk_names);
      } catch (e) {
      }
    }
    items.push({
      country: m.country || "",
      turns,
      speaker_names: names,
      spk_people: people,
      edited_by: r.edited_by || null,
      spk_src: r.spk_src || null,
      summary,
      fixed,
      trainer_acc: await txIsTrainerAcc(env, m.uid, m.first_name, m.last_name, m.p_email),
      record_id: r.ns_record_id,
      student_id: m.uid,
      student: ((m.first_name || "") + " " + (m.last_name || "")).trim(),
      category: m.category || "",
      date: ymd,
      created_at: m.created_at,
      file: m.record_file || "",
      path: txPathFill(tpl, txPathVars(m, r.ns_record_id, m.uid, m.record_file)),
      status: isNotCountedReview(m.review_details) ? "not_counted" : !m.reviewed_at ? "pending" : m.is_correct ? "correct" : "needs",
      lang: r.lang,
      dur: r.dur,
      text: r.text,
      words: words.length,
      wpm: span > 0 && words.length ? Math.round(words.length / (span / 60)) : null,
      pauses,
      longest_pause: Math.round(longest * 10) / 10
    });
  }
  return json({ ok: true, count: items.length, items });
}
__name(handleTranscriptsCorpus, "handleTranscriptsCorpus");
__name2(handleTranscriptsCorpus, "handleTranscriptsCorpus");
async function handleTranscriptsStudent(request, env, url) {
  const who = await contactAuth(env, url.searchParams.get("ns_token"), url.searchParams.get("ns_email"), url.searchParams.get("ns_server"));
  if (!who) return json({ error: "Nieprawid\u0142owy lub wygas\u0142y token NS \u2014 zaloguj si\u0119 ponownie" }, 401);
  if (who.forbidden) return json({ error: "Tylko trener/admin" }, 403);
  const sid = String(url.searchParams.get("student_id") || "");
  if (!sid) return json({ error: "Brak student_id" }, 400);
  await ensureTranscriptTables(env);
  const rs = await env.DB.prepare("SELECT * FROM record_transcript WHERE ns_student_id = ? AND status = 'ok' ORDER BY created_at DESC LIMIT 400").bind(sid).all();
  return json({ ok: true, items: (rs.results || []).map(transcriptOut) });
}
__name(handleTranscriptsStudent, "handleTranscriptsStudent");
__name2(handleTranscriptsStudent, "handleTranscriptsStudent");
async function handleTranscriptSpeakers(request, env) {
  let body = {};
  try {
    body = await request.json();
  } catch (e) {
  }
  const who = await contactAuth(env, body.ns_token, body.ns_email, body.ns_server);
  if (!who) return json({ error: "Nieprawid\u0142owy lub wygas\u0142y token NS \u2014 zaloguj si\u0119 ponownie" }, 401);
  if (who.forbidden) return json({ error: "Tylko trener/admin" }, 403);
  const rid = String(body.record_id || "");
  if (!rid) return json({ error: "Brak record_id" }, 400);
  await ensureTranscriptTables(env);
  const t = await env.DB.prepare("SELECT * FROM record_transcript WHERE ns_record_id = ?").bind(rid).first();
  if (!t || t.status !== "ok") return json({ error: "To nagranie nie ma jeszcze tekstu" }, 404);
  let words = [];
  try {
    words = JSON.parse(t.words || "[]");
  } catch (e) {
  }
  const ri = await txRecordInfo(env, rid);
  const ai = await txAiSpeakers(env, words, ri.category, t.lang, true, ri.trainerAcc);
  if (ai.src) await env.DB.prepare("UPDATE record_transcript SET speakers = ?, spk_names = ?, spk_src = ?, diar = ? WHERE ns_record_id = ?").bind(ai.speakers ? JSON.stringify(ai.speakers) : null, ai.names ? JSON.stringify(ai.names) : null, ai.src, ai.note, rid).run();
  const now = await env.DB.prepare("SELECT * FROM record_transcript WHERE ns_record_id = ?").bind(rid).first();
  return json({ ok: !!ai.speakers, note: ai.note, item: transcriptOut(now) });
}
__name(handleTranscriptSpeakers, "handleTranscriptSpeakers");
__name2(handleTranscriptSpeakers, "handleTranscriptSpeakers");
var TX_LANG_NAME = { pl: "polskim", cs: "czeskim", sk: "s\u0142owackim", en: "angielskim" };
function txDialogText(t) {
  let words = [];
  try {
    words = JSON.parse(t.words || "[]");
  } catch (e) {
  }
  let sp = null;
  try {
    sp = t.speakers ? JSON.parse(t.speakers) : null;
  } catch (e) {
  }
  let names = null;
  try {
    names = t.spk_names ? JSON.parse(t.spk_names) : null;
  } catch (e) {
  }
  if (!sp || sp.length !== words.length) return String(t.text || "");
  const out = [];
  let cur = null, buf = [];
  const flush = /* @__PURE__ */ __name(() => {
    if (buf.length) out.push((names && names[cur] ? names[cur] : "Osoba " + (cur + 1)) + ": " + buf.join(" "));
    buf = [];
  }, "flush");
  for (let i = 0; i < words.length; i++) {
    if (sp[i] !== cur) {
      flush();
      cur = sp[i];
    }
    buf.push(words[i][0]);
  }
  flush();
  return out.join("\n");
}
__name(txDialogText, "txDialogText");
__name2(txDialogText, "txDialogText");
async function txFixText(env, t, category) {
  let words = [];
  try {
    words = JSON.parse(t.words || "[]");
  } catch (e) {
  }
  let sp = null;
  try {
    sp = t.speakers ? JSON.parse(t.speakers) : null;
  } catch (e) {
  }
  if (sp && sp.length !== words.length) sp = null;
  const sents = txSentences(words);
  if (!sents.length) return null;
  const sys = 'Poprawiasz zapis z automatycznego rozpoznawania mowy (polski/czeski/s\u0142owacki). M\xF3wi\u0105 osoby, kt\xF3re si\u0119 j\u0105kaj\u0105 \u2014 w zapisie bywaj\u0105 urwane i powt\xF3rzone sylaby (\u201EZ...ra... Trancji\u201D), przekr\u0119cone s\u0142owa i brak ko\u0144c\xF3wek. Popraw KA\u017BDE zdanie tak, by brzmia\u0142o jak to, co osoba najpewniej powiedzia\u0142a: usu\u0144 urwane fragmenty i powt\xF3rzenia, popraw przekr\u0119cone s\u0142owa i gramatyk\u0119, zachowaj sens, styl i s\u0142ownictwo m\xF3wi\u0105cego. Nie dodawaj nowych tre\u015Bci, nie streszczaj, nie \u0142\u0105cz ani nie dziel zda\u0144. Je\u015Bli zdanie jest niezrozumia\u0142e \u2014 zostaw je mo\u017Cliwie blisko orygina\u0142u. Odpowiedz WY\u0141\u0104CZNIE JSON-em {"z":["poprawione zdanie 1", \u2026]} \u2014 dok\u0142adnie tyle zda\u0144, ile dosta\u0142e\u015B, w tej samej kolejno\u015Bci.';
  const out = [], BATCH = 60;
  for (let i = 0; i < sents.length; i += BATCH) {
    const part = sents.slice(i, i + BATCH);
    let fixedPart = null;
    try {
      const j = txParseJson(await txLlm(env, sys, "Kategoria: " + (category || "\u2014") + "\nZdania (" + part.length + "):\n" + part.map((x, k) => k + 1 + ". " + x.text).join("\n"), 200 + part.length * 40));
      if (j && Array.isArray(j.z) && j.z.length === part.length) fixedPart = j.z.map((x) => String(x || "").trim());
    } catch (e) {
    }
    part.forEach((x, k) => out.push([sp ? sp[x.from] : null, fixedPart && fixedPart[k] ? fixedPart[k] : x.text]));
  }
  return out;
}
__name(txFixText, "txFixText");
__name2(txFixText, "txFixText");
function txFixedDialog(t) {
  let f = null;
  try {
    f = t.fixed ? JSON.parse(t.fixed) : null;
  } catch (e) {
  }
  if (!f || !f.length) return null;
  let names = null;
  try {
    names = t.spk_names ? JSON.parse(t.spk_names) : null;
  } catch (e) {
  }
  const out = [];
  let cur, buf = [];
  const flush = /* @__PURE__ */ __name(() => {
    if (buf.length) out.push((cur === null || cur === void 0 ? "" : (names && names[cur] ? names[cur] : "Osoba " + (cur + 1)) + ": ") + buf.join(" "));
    buf = [];
  }, "flush");
  for (const [k, txt] of f) {
    if (k !== cur) {
      flush();
      cur = k;
    }
    buf.push(txt);
  }
  flush();
  return out.join("\n");
}
__name(txFixedDialog, "txFixedDialog");
__name2(txFixedDialog, "txFixedDialog");
async function txMakeSummary(env, rid, t, ri) {
  const slang = ri.lang || t.lang || "pl";
  let dialog = (txFixedDialog(t) || txDialogText(t)).trim();
  if (dialog.split(/\s+/).length < 25) return { error: "Za kr\xF3tkie nagranie na podsumowanie" };
  const LIM = 28e3;
  if (dialog.length > LIM) {
    const parts = [];
    for (let i = 0; i < dialog.length; i += LIM) parts.push(dialog.slice(i, i + LIM));
    const notes = [];
    for (let k = 0; k < parts.length; k++) {
      notes.push(String(await txLlm(
        env,
        "Streszczasz fragment zapisu rozmowy (rozpoznawanie mowy, mog\u0105 by\u0107 b\u0142\u0119dy). Wypisz po polsku w punktach wszystkie wa\u017Cne fakty, w\u0105tki, ustalenia, rady i osoby (kto co m\xF3wi). Bez wst\u0119p\xF3w.",
        "Fragment " + (k + 1) + " z " + parts.length + ":\n" + parts[k],
        900
      ) || ""));
    }
    dialog = "(Notatki z kolejnych cz\u0119\u015Bci d\u0142ugiej rozmowy)\n" + notes.join("\n\n");
  }
  const recorder = ri.trainerAcc ? "Nagranie wgra\u0142 TRENER ze swojego konta \u2014 to rozmowa trenera z kursantem (lub kursantami)." : "Nagranie wgra\u0142 kursant " + (ri.studentName || "") + " (to on nagrywa). Rozm\xF3wc\u0105 mo\u017Ce by\u0107 osoba z zewn\u0105trz (np. sprzedawca) albo inny kursant (np. w kategorii telefon do kursanta).";
  const sys = "Jeste\u015B asystentem trenera w programie Nowa Mowa (nauka p\u0142ynnego m\xF3wienia; kursanci \u0107wicz\u0105 \u201Esystemy mowy\u201D, np. zer\xF3wka, \u0142\u0105cznik, jedynka, oraz rozmowy w codziennych sytuacjach). Dostajesz zapis nagrania z rozpoznawania mowy \u2014 mog\u0105 by\u0107 b\u0142\u0119dy i przekr\u0119cone imiona/nazwy. " + recorder + ' Nie wymy\u015Blaj fakt\xF3w, kt\xF3rych nie ma w zapisie. Nigdy nie u\u017Cywaj s\u0142owa \u201Eterapia\u201D. Odpowiedz WY\u0141\u0104CZNIE JSON-em o polach: {"rozmowcy":[{"kto":"np. Trener / Karola (kursantka)","punkty":["\u2026"]}],"osoby":[{"kto":"\u2026","rola":"\u2026"}],"temat":"jedno zdanie","ustalenia":["najwa\u017Cniejsze wnioski i ustalenia, 3\u20138 punkt\xF3w"],"watki":["dodatkowe w\u0105tki"],"wniosek":"2\u20133 zdania","wiadomosci":[{"dla":"kursant nagrywaj\u0105cy | rozm\xF3wca-kursant","imie":"imi\u0119 tej osoby, je\u015Bli znane","tekst":"\u2026"}]}. Wszystko po polsku, opr\xF3cz tekst\xF3w wiadomo\u015Bci. \u201Ewiadomosci\u201D: osobna kr\xF3tka, ciep\u0142a wiadomo\u015B\u0107 od trenera dla KA\u017BDEGO kursanta bior\u0105cego udzia\u0142 w rozmowie (nie dla trenera i nie dla os\xF3b z zewn\u0105trz, np. sprzedawcy) \u2014 w j\u0119zyku ' + (TX_LANG_NAME[slang] || "polskim") + ", per \u201ETy\u201D, 3\u20136 punkt\xF3w: co posz\u0142o dobrze, nad czym pracowa\u0107, konkretne nast\u0119pne kroki. Pomi\u0144 sprawy zdrowotne, rodzinne, szczeg\xF3\u0142y o innych ludziach i wszystko prywatne \u2014 tylko mowa, \u0107wiczenia, post\u0119py i plan. Je\u015Bli m\xF3wi jedna osoba (monolog), rozmowcy ma jedn\u0105 pozycj\u0119, a wiadomosci jedn\u0105 \u2014 dla nagrywaj\u0105cego.";
  const raw = await txLlm(env, sys, "Kategoria nagrania: " + (ri.category || "\u2014") + "\n\nZapis:\n" + dialog, 2600);
  const j = txParseJson(raw);
  if (!j || !j.ustalenia && !j.temat) return { error: "AI nie zwr\xF3ci\u0142o czytelnego podsumowania \u2014 spr\xF3buj ponownie" };
  if (!Array.isArray(j.wiadomosci)) j.wiadomosci = j.dla_kursanta ? [{ dla: ri.trainerAcc ? "kursant" : "kursant nagrywaj\u0105cy", imie: j.kursant_imie || "", tekst: Array.isArray(j.dla_kursanta) ? j.dla_kursanta.map((x) => "\u2022 " + x).join("\n") : String(j.dla_kursanta) }] : [];
  j.wiadomosci = j.wiadomosci.map((m) => ({ dla: String(m.dla || ""), imie: String(m.imie || ""), tekst: Array.isArray(m.tekst) ? m.tekst.map((x) => "\u2022 " + x).join("\n") : String(m.tekst || "") })).filter((m) => m.tekst);
  return { summary: { ...j, lang: slang, trainer_acc: !!ri.trainerAcc, owner_name: ri.studentName || "", created_at: Math.floor(Date.now() / 1e3) } };
}
__name(txMakeSummary, "txMakeSummary");
__name2(txMakeSummary, "txMakeSummary");
async function txAuthBody(request, env) {
  let body = {};
  try {
    body = await request.json();
  } catch (e) {
  }
  const who = await contactAuth(env, body.ns_token, body.ns_email, body.ns_server);
  if (!who) return { err: json({ error: "Nieprawid\u0142owy lub wygas\u0142y token NS \u2014 zaloguj si\u0119 ponownie" }, 401) };
  if (who.forbidden) return { err: json({ error: "Tylko trener/admin" }, 403) };
  return { body, who };
}
__name(txAuthBody, "txAuthBody");
__name2(txAuthBody, "txAuthBody");
async function handleTranscriptSummary(request, env) {
  const { body, err } = await txAuthBody(request, env);
  if (err) return err;
  const rid = String(body.record_id || "");
  if (!rid) return json({ error: "Brak record_id" }, 400);
  if (!env.AI) return json({ error: "Brak Workers AI" }, 500);
  await ensureTranscriptTables(env);
  const t = await env.DB.prepare("SELECT * FROM record_transcript WHERE ns_record_id = ?").bind(rid).first();
  if (!t || t.status !== "ok") return json({ error: "To nagranie nie ma jeszcze tekstu" }, 404);
  if (t.summary && !body.force) {
    try {
      const c = JSON.parse(t.summary);
      if (!c.error) return json({ ok: true, summary: c, cached: true });
    } catch (e) {
    }
  }
  const ri = await txRecordInfo(env, rid);
  const r = await txMakeSummary(env, rid, t, ri);
  if (r.error) return json({ error: r.error }, r.error.indexOf("kr\xF3tkie") > 0 ? 400 : 502);
  await env.DB.prepare("UPDATE record_transcript SET summary = ? WHERE ns_record_id = ?").bind(JSON.stringify(r.summary), rid).run();
  return json({ ok: true, summary: r.summary });
}
__name(handleTranscriptSummary, "handleTranscriptSummary");
__name2(handleTranscriptSummary, "handleTranscriptSummary");
async function handleTranscriptFix(request, env) {
  const { body, err } = await txAuthBody(request, env);
  if (err) return err;
  const rid = String(body.record_id || "");
  if (!rid) return json({ error: "Brak record_id" }, 400);
  await ensureTranscriptTables(env);
  const t = await env.DB.prepare("SELECT * FROM record_transcript WHERE ns_record_id = ?").bind(rid).first();
  if (!t || t.status !== "ok") return json({ error: "To nagranie nie ma jeszcze tekstu" }, 404);
  const ri = await txRecordInfo(env, rid);
  const f = await txFixText(env, t, ri.category);
  if (!f) return json({ error: "Brak tekstu do poprawy" }, 400);
  await env.DB.prepare("UPDATE record_transcript SET fixed = ? WHERE ns_record_id = ?").bind(JSON.stringify(f), rid).run();
  return json({ ok: true, item: transcriptOut(await env.DB.prepare("SELECT * FROM record_transcript WHERE ns_record_id = ?").bind(rid).first()) });
}
__name(handleTranscriptFix, "handleTranscriptFix");
__name2(handleTranscriptFix, "handleTranscriptFix");
async function handleTranscriptEdit(request, env) {
  const { body, err } = await txAuthBody(request, env);
  if (err) return err;
  const rid = String(body.record_id || "");
  if (!rid) return json({ error: "Brak record_id" }, 400);
  await ensureTranscriptTables(env);
  const t = await env.DB.prepare("SELECT * FROM record_transcript WHERE ns_record_id = ?").bind(rid).first();
  if (!t || t.status !== "ok") return json({ error: "To nagranie nie ma tekstu" }, 404);
  const sets = [], vals = [];
  if (Array.isArray(body.fixed)) {
    const f = body.fixed.map((x) => [x[0] === null || x[0] === void 0 || x[0] === "" ? null : Math.max(0, Math.min(5, parseInt(x[0], 10) || 0)), String(x[1] || "").slice(0, 5e3)]).filter((x) => x[1].trim());
    sets.push("fixed = ?");
    vals.push(JSON.stringify(f));
  }
  if (body.spk_names && typeof body.spk_names === "object") {
    const n = {};
    for (const k of Object.keys(body.spk_names)) if (String(body.spk_names[k] || "").trim()) n[k] = String(body.spk_names[k]).trim().slice(0, 60);
    sets.push("spk_names = ?");
    vals.push(JSON.stringify(n));
  }
  if (body.spk_people && typeof body.spk_people === "object") {
    const pp = {};
    for (const k of Object.keys(body.spk_people)) if (body.spk_people[k]) pp[k] = String(body.spk_people[k]);
    sets.push("spk_people = ?");
    vals.push(JSON.stringify(pp));
  }
  if (!sets.length) return json({ error: "Brak zmian" }, 400);
  sets.push("edited_by = ?");
  vals.push(String(body.ns_email || "trener"));
  await env.DB.prepare("UPDATE record_transcript SET " + sets.join(", ") + " WHERE ns_record_id = ?").bind(...vals, rid).run();
  return json({ ok: true, item: transcriptOut(await env.DB.prepare("SELECT * FROM record_transcript WHERE ns_record_id = ?").bind(rid).first()) });
}
__name(handleTranscriptEdit, "handleTranscriptEdit");
__name2(handleTranscriptEdit, "handleTranscriptEdit");
async function handleTranscriptDelete(request, env) {
  const { body, err } = await txAuthBody(request, env);
  if (err) return err;
  const rid = String(body.record_id || "");
  if (!rid) return json({ error: "Brak record_id" }, 400);
  await ensureTranscriptTables(env);
  await env.DB.prepare("UPDATE record_transcript SET status = 'deleted', text = '', words = '[]', speakers = NULL, spk_names = NULL, spk_src = 'deleted', summary = NULL, fixed = NULL, error = ? WHERE ns_record_id = ?").bind("usuni\u0119ty przez " + (body.ns_email || "trenera"), rid).run();
  return json({ ok: true });
}
__name(handleTranscriptDelete, "handleTranscriptDelete");
__name2(handleTranscriptDelete, "handleTranscriptDelete");
async function handleTranscriptRun(request, env) {
  let body = {};
  try {
    body = await request.json();
  } catch (e) {
  }
  const who = await contactAuth(env, body.ns_token, body.ns_email, body.ns_server);
  if (!who) return json({ error: "Nieprawid\u0142owy lub wygas\u0142y token NS \u2014 zaloguj si\u0119 ponownie" }, 401);
  if (who.forbidden) return json({ error: "Tylko trener/admin" }, 403);
  const rid = String(body.record_id || ""), path = String(body.path || "");
  if (!rid || !path.startsWith("/uploads/")) return json({ error: "Brak record_id albo \u015Bcie\u017Cki pliku" }, 400);
  await ensureTranscriptTables(env);
  const have = await env.DB.prepare("SELECT * FROM record_transcript WHERE ns_record_id = ?").bind(rid).first();
  let reqFile = "";
  try {
    reqFile = decodeURIComponent(path.split("/").pop() || "");
  } catch (e) {
    reqFile = path.split("/").pop() || "";
  }
  const sameFile = !have || !have.src_file || !reqFile || have.src_file === reqFile;
  if (have && have.status === "ok" && !body.force && sameFile && /\|v[345]-/.test(String(have.model || ""))) return json({ ok: true, item: transcriptOut(have) });
  const a = await fetch(nsServerBase(body.ns_server) + path, { headers: { "Authorization": "Token token=" + body.ns_token, "Email": body.ns_email } });
  if (!a.ok) return json({ error: "Nie uda\u0142o si\u0119 pobra\u0107 pliku z NS (" + a.status + ")" }, 502);
  const ri = await txRecordInfo(env, rid);
  try {
    const tpl = ri.vars ? txPathTemplate(path, ri.vars) : null;
    if (tpl && tpl !== await txPathTpl(env)) {
      await env.DB.prepare("INSERT INTO tx_kv (k, v) VALUES ('path_tpl2', ?) ON CONFLICT(k) DO UPDATE SET v = excluded.v").bind(tpl).run();
      await env.DB.prepare("DELETE FROM record_transcript WHERE status = 'error' AND error LIKE 'Nie uda\u0142o si\u0119 pobra\u0107 pliku%'").run();
    }
  } catch (e) {
  }
  const item = await transcribeAndStore(env, rid, body.student_id, await a.arrayBuffer(), ri.lang, ri.category, ri.trainerAcc, ri.file);
  if (item.status !== "ok") return json({ ok: false, error: item.error, item: transcriptOut(item) }, item.status === "limit" ? 429 : 502);
  return json({ ok: true, item: transcriptOut(item), diar: item.diar || null, category: ri.category || null });
}
__name(handleTranscriptRun, "handleTranscriptRun");
__name2(handleTranscriptRun, "handleTranscriptRun");
async function handleRecordFpList(request, env, url) {
  const who = await contactAuth(env, url.searchParams.get("ns_token"), url.searchParams.get("ns_email"), url.searchParams.get("ns_server"));
  if (!who) return json({ error: "Nieprawid\u0142owy lub wygas\u0142y token NS \u2014 zaloguj si\u0119 ponownie" }, 401);
  if (who.forbidden) return json({ error: "Tylko trener/admin" }, 403);
  const sid = String(url.searchParams.get("student_id") || "");
  if (!sid) return json({ error: "Brak student_id" }, 400);
  await ensureCoachTables(env);
  const rs = await env.DB.prepare("SELECT ns_record_id, sha256, dur, rec_ts, env FROM record_fp WHERE ns_student_id = ? ORDER BY rec_ts DESC LIMIT 500").bind(sid).all();
  return json({ ok: true, fps: (rs.results || []).map((r) => ({ record_id: r.ns_record_id, sha256: r.sha256, dur: r.dur, rec_ts: r.rec_ts, env: r.env })) });
}
__name(handleRecordFpList, "handleRecordFpList");
__name2(handleRecordFpList, "handleRecordFpList");
async function handleRecordFpSave(request, env) {
  let body = {};
  try {
    body = await request.json();
  } catch (e) {
  }
  const who = await contactAuth(env, body.ns_token, body.ns_email, body.ns_server);
  if (!who) return json({ error: "Nieprawid\u0142owy lub wygas\u0142y token NS \u2014 zaloguj si\u0119 ponownie" }, 401);
  if (who.forbidden) return json({ error: "Tylko trener/admin" }, 403);
  const rid = String(body.record_id || ""), sid = String(body.student_id || "");
  const envStr = String(body.env || "");
  if (!rid || !sid || !envStr || envStr.length > 4e4) return json({ error: "Brak danych odcisku" }, 400);
  await ensureCoachTables(env);
  await env.DB.prepare("INSERT INTO record_fp (ns_record_id, ns_student_id, sha256, dur, rec_ts, env, created_at) VALUES (?, ?, ?, ?, ?, ?, ?) ON CONFLICT(ns_record_id) DO NOTHING").bind(rid, sid, String(body.sha256 || "").slice(0, 64), Number(body.dur) || 0, parseInt(body.rec_ts, 10) || null, envStr, Math.floor(Date.now() / 1e3)).run();
  return json({ ok: true });
}
__name(handleRecordFpSave, "handleRecordFpSave");
__name2(handleRecordFpSave, "handleRecordFpSave");
async function handleRecordSystemGet(request, env, url) {
  const who = await contactAuth(env, url.searchParams.get("ns_token"), url.searchParams.get("ns_email"), url.searchParams.get("ns_server"));
  if (!who) return json({ error: "Nieprawid\u0142owy lub wygas\u0142y token NS \u2014 zaloguj si\u0119 ponownie" }, 401);
  if (who.forbidden) return json({ error: "Tylko trener/admin" }, 403);
  await ensureCoachTables(env);
  const ids = String(url.searchParams.get("record_ids") || "").split(",").map((x) => x.trim()).filter(Boolean).slice(0, 500);
  const overrides = {};
  for (let i = 0; i < ids.length; i += 90) {
    const chunk = ids.slice(i, i + 90);
    const rs = await env.DB.prepare(`SELECT ns_record_id, system, student_system, set_by, set_at FROM record_system_override WHERE ns_record_id IN (${chunk.map(() => "?").join(",")})`).bind(...chunk).all();
    for (const r of rs.results || []) overrides[r.ns_record_id] = { system: r.system, student_system: r.student_system, set_by: r.set_by, set_at: r.set_at };
  }
  return json({ ok: true, overrides });
}
__name(handleRecordSystemGet, "handleRecordSystemGet");
__name2(handleRecordSystemGet, "handleRecordSystemGet");
async function handleRecordSystemSave(request, env) {
  let body = {};
  try {
    body = await request.json();
  } catch (e) {
  }
  const who = await contactAuth(env, body.ns_token, body.ns_email, body.ns_server);
  if (!who) return json({ error: "Nieprawid\u0142owy lub wygas\u0142y token NS \u2014 zaloguj si\u0119 ponownie" }, 401);
  if (who.forbidden) return json({ error: "Tylko trener/admin" }, 403);
  const rid = String(body.record_id || "").trim();
  if (!rid) return json({ error: "Brak record_id" }, 400);
  const system = String(body.system || "").trim().slice(0, 40);
  await ensureCoachTables(env);
  if (!system) {
    await env.DB.prepare("DELETE FROM record_system_override WHERE ns_record_id = ?").bind(rid).run();
    return json({ ok: true, override: null });
  }
  const now = Math.floor(Date.now() / 1e3);
  const studentSystem = body.student_system ? String(body.student_system).slice(0, 40) : null;
  const by = String(who.email).toLowerCase();
  await env.DB.prepare(`INSERT INTO record_system_override (ns_record_id, ns_student_id, system, student_system, set_by, set_at) VALUES (?, ?, ?, ?, ?, ?)
    ON CONFLICT(ns_record_id) DO UPDATE SET system = excluded.system, student_system = COALESCE(record_system_override.student_system, excluded.student_system), set_by = excluded.set_by, set_at = excluded.set_at`).bind(rid, body.student_id ? String(body.student_id) : null, system, studentSystem, by, now).run();
  const r = await env.DB.prepare("SELECT system, student_system, set_by, set_at FROM record_system_override WHERE ns_record_id = ?").bind(rid).first();
  return json({ ok: true, override: r });
}
__name(handleRecordSystemSave, "handleRecordSystemSave");
__name2(handleRecordSystemSave, "handleRecordSystemSave");
async function handleReviewLogCounts(request, env, url) {
  const who = await contactAuth(env, url.searchParams.get("ns_token"), url.searchParams.get("ns_email"), url.searchParams.get("ns_server"));
  if (!who) return json({ error: "Nieprawid\u0142owy lub wygas\u0142y token NS \u2014 zaloguj si\u0119 ponownie" }, 401);
  if (who.forbidden) return json({ error: "Tylko trener/admin" }, 403);
  const ids = String(url.searchParams.get("record_ids") || "").split(",").map((x) => x.trim()).filter(Boolean).slice(0, 300);
  if (!ids.length) return json({ ok: true, counts: {} });
  await ensureCoachTables(env);
  const ph = ids.map(() => "?").join(",");
  const rows = (await env.DB.prepare(`SELECT ns_record_id AS id, SUM(CASE WHEN is_correct = 0 THEN 1 ELSE 0 END) AS n FROM review_log WHERE ns_record_id IN (${ph}) GROUP BY ns_record_id`).bind(...ids).all()).results || [];
  const counts = {};
  rows.forEach((r) => {
    counts[r.id] = Number(r.n) || 0;
  });
  return json({ ok: true, counts });
}
__name(handleReviewLogCounts, "handleReviewLogCounts");
__name2(handleReviewLogCounts, "handleReviewLogCounts");
function coachNoticeOut(r) {
  return { id: r.id, student_id: r.ns_student_id, record_id: r.ns_record_id, criteria: coachJsonArr(r.criteria), correction_no: r.correction_no, created_by: r.created_by, created_at: r.created_at, acked_at: r.acked_at };
}
__name(coachNoticeOut, "coachNoticeOut");
async function handleCallNoticesList(request, env, url) {
  const who = await contactAuth(env, url.searchParams.get("ns_token"), url.searchParams.get("ns_email"), url.searchParams.get("ns_server"));
  if (!who) return json({ error: "Nieprawid\u0142owy lub wygas\u0142y token NS \u2014 zaloguj si\u0119 ponownie" }, 401);
  if (who.forbidden) return json({ error: "Tylko trener/admin" }, 403);
  await ensureCoachTables(env);
  const sid = url.searchParams.get("student_id");
  const rows = sid ? (await env.DB.prepare("SELECT * FROM call_notice WHERE ns_student_id = ? AND cleared_at IS NULL ORDER BY created_at DESC").bind(String(sid)).all()).results || [] : (await env.DB.prepare("SELECT * FROM call_notice WHERE cleared_at IS NULL ORDER BY created_at DESC LIMIT 200").all()).results || [];
  return json({ ok: true, notices: rows.map(coachNoticeOut) });
}
__name(handleCallNoticesList, "handleCallNoticesList");
__name2(handleCallNoticesList, "handleCallNoticesList");
async function handleCallNoticesClear(request, env) {
  let body = {};
  try {
    body = await request.json();
  } catch (e) {
  }
  const who = await contactAuth(env, body.ns_token, body.ns_email, body.ns_server);
  if (!who) return json({ error: "Nieprawid\u0142owy lub wygas\u0142y token NS \u2014 zaloguj si\u0119 ponownie" }, 401);
  if (who.forbidden) return json({ error: "Tylko trener/admin" }, 403);
  await ensureCoachTables(env);
  const now = Math.floor(Date.now() / 1e3);
  if (body.id) await env.DB.prepare("UPDATE call_notice SET cleared_at = ?, cleared_by = ? WHERE id = ? AND cleared_at IS NULL").bind(now, who.email, Number(body.id)).run();
  else if (body.student_id) await env.DB.prepare("UPDATE call_notice SET cleared_at = ?, cleared_by = ? WHERE ns_student_id = ? AND cleared_at IS NULL").bind(now, who.email, String(body.student_id)).run();
  else return json({ error: "Brak id albo student_id" }, 400);
  return json({ ok: true });
}
__name(handleCallNoticesClear, "handleCallNoticesClear");
__name2(handleCallNoticesClear, "handleCallNoticesClear");
async function handleStudentHome(request, env, url, ctx) {
  const me = await coachStudentFromRequest(request, env, url, null);
  if (!me) return json({ error: "Nieprawid\u0142owy lub wygas\u0142y token NS \u2014 zaloguj si\u0119 ponownie" }, 401);
  const out = { ok: true, notices: [], focus: null, corrections: null, special_done: null };
  const jobs = [];
  if (me.id) {
    jobs.push((async () => {
      await ensureCoachTables(env);
      const rows = (await env.DB.prepare("SELECT * FROM call_notice WHERE ns_student_id = ? AND cleared_at IS NULL ORDER BY created_at DESC").bind(me.id).all()).results || [];
      out.notices = rows.map(coachNoticeOut);
      const f = await env.DB.prepare("SELECT * FROM student_focus WHERE ns_student_id = ?").bind(me.id).first();
      out.focus = coachFocusOut(f);
    })());
  }
  jobs.push((async () => {
    try {
      const r = await handleSpecialTasksMine(request, env, url);
      const d = await r.json();
      if (d && d.ok) out.special_done = d.done || [];
    } catch (e) {
    }
  })());
  jobs.push((async () => {
    try {
      const u = await getOrCreateUserByNsEmail(env, me.email, null);
      const syncState = await env.DB.prepare("SELECT last_synced_at FROM needs_correction_sync_state WHERE user_id = ?").bind(u.id).first();
      if (!syncState) await syncNeedsCorrectionFromPostgres(env, u.id, u.email);
      else if (Date.now() - syncState.last_synced_at * 1e3 > 12e4) speedLater(syncNeedsCorrectionFromPostgres(env, u.id, u.email));
      const { results } = await env.DB.prepare("SELECT * FROM needs_correction_cache WHERE user_id = ? ORDER BY COALESCE(recorded_at, reviewed_at) ASC").bind(u.id).all();
      out.corrections = { count: results.length, entries: results.map(correctionEntryOut) };
    } catch (e) {
    }
  })());
  await Promise.all(jobs);
  return json(out);
}
__name(handleStudentHome, "handleStudentHome");
__name2(handleStudentHome, "handleStudentHome");
async function handleCallNoticesMine(request, env, url) {
  const me = await coachStudentFromRequest(request, env, url, null);
  if (!me) return json({ error: "Nieprawid\u0142owy lub wygas\u0142y token NS \u2014 zaloguj si\u0119 ponownie" }, 401);
  if (!me.id) return json({ ok: true, notices: [] });
  await ensureCoachTables(env);
  const rows = (await env.DB.prepare("SELECT * FROM call_notice WHERE ns_student_id = ? AND cleared_at IS NULL ORDER BY created_at DESC").bind(me.id).all()).results || [];
  return json({ ok: true, notices: rows.map(coachNoticeOut) });
}
__name(handleCallNoticesMine, "handleCallNoticesMine");
__name2(handleCallNoticesMine, "handleCallNoticesMine");
async function handleCallNoticesAck(request, env) {
  let body = {};
  try {
    body = await request.json();
  } catch (e) {
  }
  const me = await coachStudentFromRequest(request, env, null, body);
  if (!me) return json({ error: "Nieprawid\u0142owy lub wygas\u0142y token NS \u2014 zaloguj si\u0119 ponownie" }, 401);
  if (!me.id || !body.id) return json({ ok: true });
  await ensureCoachTables(env);
  await env.DB.prepare("UPDATE call_notice SET acked_at = ? WHERE id = ? AND ns_student_id = ? AND acked_at IS NULL").bind(Math.floor(Date.now() / 1e3), Number(body.id), me.id).run();
  return json({ ok: true });
}
__name(handleCallNoticesAck, "handleCallNoticesAck");
__name2(handleCallNoticesAck, "handleCallNoticesAck");
function coachFocusOut(r) {
  if (!r) return null;
  return { criteria: coachJsonArr(r.criteria), note: r.note || "", achieved: coachJsonArr(r.achieved), set_by: r.set_by, set_at: r.set_at };
}
__name(coachFocusOut, "coachFocusOut");
async function handleFocusGet(request, env, url) {
  const who = await contactAuth(env, url.searchParams.get("ns_token"), url.searchParams.get("ns_email"), url.searchParams.get("ns_server"));
  if (!who) return json({ error: "Nieprawid\u0142owy lub wygas\u0142y token NS \u2014 zaloguj si\u0119 ponownie" }, 401);
  if (who.forbidden) return json({ error: "Tylko trener/admin" }, 403);
  const sid = url.searchParams.get("student_id");
  if (!sid) return json({ error: "Brak student_id" }, 400);
  await ensureCoachTables(env);
  const r = await env.DB.prepare("SELECT * FROM student_focus WHERE ns_student_id = ?").bind(String(sid)).first();
  return json({ ok: true, focus: coachFocusOut(r) });
}
__name(handleFocusGet, "handleFocusGet");
__name2(handleFocusGet, "handleFocusGet");
async function handleFocusSave(request, env) {
  let body = {};
  try {
    body = await request.json();
  } catch (e) {
  }
  const who = await contactAuth(env, body.ns_token, body.ns_email, body.ns_server);
  if (!who) return json({ error: "Nieprawid\u0142owy lub wygas\u0142y token NS \u2014 zaloguj si\u0119 ponownie" }, 401);
  if (who.forbidden) return json({ error: "Tylko trener/admin" }, 403);
  const sid = String(body.student_id || "");
  if (!sid) return json({ error: "Brak student_id" }, 400);
  await ensureCoachTables(env);
  const crit = (Array.isArray(body.criteria) ? body.criteria : []).slice(0, 3).map((c) => ({ id: String(c.id || ""), name: String(c.name || "").slice(0, 60) })).filter((c) => c.name);
  const ach = (Array.isArray(body.achieved) ? body.achieved : []).slice(-20).map((a) => ({ name: String(a.name || "").slice(0, 60), at: Number(a.at) || Math.floor(Date.now() / 1e3) }));
  const now = Math.floor(Date.now() / 1e3);
  await env.DB.prepare(`INSERT INTO student_focus (ns_student_id, criteria, note, achieved, set_by, set_at) VALUES (?, ?, ?, ?, ?, ?)
    ON CONFLICT(ns_student_id) DO UPDATE SET criteria = excluded.criteria, note = excluded.note, achieved = excluded.achieved, set_by = excluded.set_by, set_at = excluded.set_at`).bind(sid, JSON.stringify(crit), String(body.note || "").slice(0, 300), JSON.stringify(ach), who.email, now).run();
  return json({ ok: true, focus: { criteria: crit, note: String(body.note || "").slice(0, 300), achieved: ach, set_by: who.email, set_at: now } });
}
__name(handleFocusSave, "handleFocusSave");
__name2(handleFocusSave, "handleFocusSave");
async function handleFocusMine(request, env, url) {
  const me = await coachStudentFromRequest(request, env, url, null);
  if (!me) return json({ error: "Nieprawid\u0142owy lub wygas\u0142y token NS \u2014 zaloguj si\u0119 ponownie" }, 401);
  if (!me.id) return json({ ok: true, focus: null });
  await ensureCoachTables(env);
  const r = await env.DB.prepare("SELECT * FROM student_focus WHERE ns_student_id = ?").bind(me.id).first();
  return json({ ok: true, focus: coachFocusOut(r) });
}
__name(handleFocusMine, "handleFocusMine");
__name2(handleFocusMine, "handleFocusMine");
function callDaysCc(env) {
  return String(env.CALL_DAYS_CC === void 0 ? "jakanie@gmail.com" : env.CALL_DAYS_CC).split(",").map((x) => x.trim()).filter(Boolean);
}
__name(callDaysCc, "callDaysCc");
async function nsUserEmailById(env, id) {
  if (!id) return null;
  const pgClient = new Client({ connectionString: env.HYPERDRIVE.connectionString });
  try {
    await pgClient.connect();
    const r = await pgClient.query(`SELECT email FROM users WHERE id = $1 AND deleted_at IS NULL LIMIT 1`, [String(id)]);
    return r.rows[0] ? r.rows[0].email : null;
  } catch (e) {
    return null;
  } finally {
    try {
      await pgClient.end();
    } catch (e) {
    }
  }
}
__name(nsUserEmailById, "nsUserEmailById");
__name2(nsUserEmailById, "nsUserEmailById");
async function gcalAuth(env) {
  const o = await googleOAuthAccessToken(env);
  if (o) return { token: o, mode: "oauth" };
  const sa = await googleAccessToken(env);
  if (sa) return { token: sa, mode: "service" };
  return null;
}
__name(gcalAuth, "gcalAuth");
async function googleAccessToken(env) {
  if (!env.GOOGLE_SA_EMAIL || !env.GOOGLE_SA_KEY) return null;
  const pem = String(env.GOOGLE_SA_KEY).replace(/\\n/g, "\n").replace(/-----[^-]+-----/g, "").replace(/\s+/g, "");
  const der = Uint8Array.from(atob(pem), (c) => c.charCodeAt(0));
  const key = await crypto.subtle.importKey("pkcs8", der, { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" }, false, ["sign"]);
  const now = Math.floor(Date.now() / 1e3);
  const head = b64url(JSON.stringify({ alg: "RS256", typ: "JWT" }));
  const claim = b64url(JSON.stringify({ iss: env.GOOGLE_SA_EMAIL, scope: "https://www.googleapis.com/auth/calendar.events", aud: "https://oauth2.googleapis.com/token", iat: now, exp: now + 3600 }));
  const sig = await crypto.subtle.sign("RSASSA-PKCS1-v1_5", key, new TextEncoder().encode(head + "." + claim));
  const r = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: "grant_type=" + encodeURIComponent("urn:ietf:params:oauth:grant-type:jwt-bearer") + "&assertion=" + head + "." + claim + "." + b64url(sig)
  });
  const d = await r.json().catch(() => null);
  return d && d.access_token ? d.access_token : null;
}
__name(googleAccessToken, "googleAccessToken");
__name2(googleAccessToken, "googleAccessToken");
function gcalCalendarId(env, mode) {
  return env.GOOGLE_CALENDAR_ID || (mode === "oauth" ? "primary" : "jakanie@gmail.com");
}
__name(gcalCalendarId, "gcalCalendarId");
__name2(gcalCalendarId, "gcalCalendarId");
async function gcalInsert(env, row) {
  const auth = await gcalAuth(env);
  if (!auth) return { status: "not_configured" };
  const tok = auth.token;
  const tz = "Europe/Warsaw";
  let start, end;
  if (row.time && /^\d{1,2}:\d{2}$/.test(row.time)) {
    const [h, m] = row.time.split(":").map(Number);
    const e = h * 60 + m + 15;
    const pad = /* @__PURE__ */ __name((n) => String(n).padStart(2, "0"), "pad");
    start = { dateTime: `${row.day}T${pad(h)}:${pad(m)}:00`, timeZone: tz };
    end = { dateTime: `${row.day}T${pad(Math.floor(e / 60) % 24)}:${pad(e % 60)}:00`, timeZone: tz };
  } else {
    const nd = /* @__PURE__ */ new Date(row.day + "T00:00:00Z");
    nd.setUTCDate(nd.getUTCDate() + 1);
    start = { date: row.day };
    end = { date: nd.toISOString().slice(0, 10) };
  }
  const body = {
    summary: `\u{1F4DE} Telefon od kursanta: ${row.student_name || row.ns_student_id}`,
    description: [row.note ? `Notatka: ${row.note}` : "", row.student_phone ? `Tel. kursanta: ${row.student_phone}` : "", `Wyznaczy\u0142(a): ${row.trainer_name || row.trainer_email}`, "Dodane z panelu trenera NewSpeech"].filter(Boolean).join("\n"),
    start,
    end,
    reminders: { useDefault: false, overrides: [{ method: "popup", minutes: 30 }] }
  };
  let invited = [];
  if (auth.mode === "oauth") {
    body.summary = `\u{1F4DE} Telefon do trenera: ${row.student_name || "kursant"} \u2192 ${row.trainer_name || row.trainer_email}`;
    const guests = [row.student_email, row.trainer_email].concat(callDaysCc(env)).map((e) => String(e || "").trim().toLowerCase()).filter((e) => /@/.test(e));
    invited = [...new Set(guests)];
    if (invited.length) {
      body.attendees = invited.map((email) => ({ email }));
      body.guestsCanModify = false;
      body.description += `
Telefon trenera: ${row.trainer_phone || "\u2014"}`;
    }
  }
  const qs = auth.mode === "oauth" ? "?sendUpdates=all" : "";
  const r = await fetch(`https://www.googleapis.com/calendar/v3/calendars/${encodeURIComponent(gcalCalendarId(env, auth.mode))}/events${qs}`, {
    method: "POST",
    headers: { "Authorization": "Bearer " + tok, "Content-Type": "application/json" },
    body: JSON.stringify(body)
  });
  const d = await r.json().catch(() => null);
  if (!r.ok || !d || !d.id) return { status: "error", error: d && d.error && d.error.message || "HTTP " + r.status };
  return { status: "ok", id: d.id, invited, student_invited: !!(row.student_email && invited.indexOf(String(row.student_email).toLowerCase()) !== -1) };
}
__name(gcalInsert, "gcalInsert");
__name2(gcalInsert, "gcalInsert");
async function gcalDelete(env, eventId) {
  if (!eventId) return;
  const auth = await gcalAuth(env);
  if (!auth) return;
  const tok = auth.token;
  await fetch(`https://www.googleapis.com/calendar/v3/calendars/${encodeURIComponent(gcalCalendarId(env, auth.mode))}/events/${encodeURIComponent(eventId)}${auth.mode === "oauth" ? "?sendUpdates=all" : ""}`, {
    method: "DELETE",
    headers: { "Authorization": "Bearer " + tok }
  }).catch(() => {
  });
}
__name(gcalDelete, "gcalDelete");
__name2(gcalDelete, "gcalDelete");
async function handleCallDaysList(request, env, url) {
  const who = await contactAuth(env, url.searchParams.get("ns_token"), url.searchParams.get("ns_email"), url.searchParams.get("ns_server"));
  if (!who) return json({ error: "Nieprawid\u0142owy lub wygas\u0142y token NS \u2014 zaloguj si\u0119 ponownie" }, 401);
  if (who.forbidden) return json({ error: "Dni telefon\xF3w wyznacza trener" }, 403);
  await ensureCallDaysTable(env);
  const sid = url.searchParams.get("ns_student_id");
  const rows = sid ? (await env.DB.prepare("SELECT * FROM call_days WHERE ns_student_id = ? ORDER BY day, time").bind(String(sid)).all()).results || [] : (await env.DB.prepare("SELECT * FROM call_days WHERE day >= ? ORDER BY day, time LIMIT 1000").bind(new Date(Date.now() - 60 * 864e5).toISOString().slice(0, 10)).all()).results || [];
  await ensureGoogleTables(env);
  const oauthConnected = !!(env.GOOGLE_OAUTH_CLIENT_ID && env.GOOGLE_OAUTH_CLIENT_SECRET && await appSettingGet(env, "google_refresh_token"));
  const account = oauthConnected ? await appSettingGet(env, "google_account") : null;
  return json({ ok: true, days: rows, gcal_configured: oauthConnected || !!(env.GOOGLE_SA_EMAIL && env.GOOGLE_SA_KEY), gcal_mode: oauthConnected ? "oauth" : env.GOOGLE_SA_EMAIL && env.GOOGLE_SA_KEY ? "service" : null, calendar_id: account || gcalCalendarId(env, oauthConnected ? "oauth" : null), invites: oauthConnected });
}
__name(handleCallDaysList, "handleCallDaysList");
__name2(handleCallDaysList, "handleCallDaysList");
async function handleCallDaysAction(request, env, action) {
  let body = {};
  try {
    body = await request.json();
  } catch (e) {
  }
  const who = await contactAuth(env, body.ns_token, body.ns_email, body.ns_server);
  if (!who) return json({ error: "Nieprawid\u0142owy lub wygas\u0142y token NS \u2014 zaloguj si\u0119 ponownie" }, 401);
  if (who.forbidden) return json({ error: "Dni telefon\xF3w wyznacza trener" }, 403);
  await ensureCallDaysTable(env);
  if (action === "save") {
    const days = (Array.isArray(body.days) ? body.days : [body.day]).map((d) => String(d || "").slice(0, 10)).filter((d) => /^\d{4}-\d{2}-\d{2}$/.test(d));
    if (!body.ns_student_id || !days.length) return json({ error: "Podaj kursanta i co najmniej jeden dzie\u0144" }, 400);
    const time = body.time && /^\d{1,2}:\d{2}$/.test(String(body.time)) ? String(body.time) : null;
    const now = Math.floor(Date.now() / 1e3), out = [];
    const studentEmail = await nsUserEmailById(env, body.ns_student_id);
    for (const day of days) {
      const dup = await env.DB.prepare("SELECT id FROM call_days WHERE ns_student_id = ? AND day = ? AND IFNULL(time,'') = IFNULL(?, '')").bind(String(body.ns_student_id), day, time).first();
      if (dup) {
        out.push({ day, id: dup.id, duplicate: true });
        continue;
      }
      const row = { ns_student_id: String(body.ns_student_id), student_name: String(body.student_name || "").trim(), student_phone: String(body.student_phone || "").trim(), day, time, note: String(body.note || "").trim() || null, trainer_email: who.email, trainer_name: String(body.trainer_name || "").trim() || null, trainer_phone: String(body.trainer_phone || "").trim() || null, student_email: studentEmail };
      let g = { status: "error" };
      try {
        g = await gcalInsert(env, row);
      } catch (e) {
        g = { status: "error", error: e.message };
      }
      const r = await env.DB.prepare("INSERT INTO call_days (ns_student_id, student_name, day, time, note, trainer_email, trainer_name, trainer_phone, gcal_event_id, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)").bind(row.ns_student_id, row.student_name || null, day, time, row.note, row.trainer_email, row.trainer_name, row.trainer_phone, g.id || null, now).run();
      out.push({ day, id: r.meta.last_row_id, gcal: g.status, gcal_error: g.error || null, invited: g.invited || [], student_gcal: g.status !== "ok" ? null : g.student_invited ? "invited" : studentEmail ? "not_invited" : "no_email" });
    }
    return json({ ok: true, saved: out });
  }
  if (action === "mark") {
    const row = await env.DB.prepare("SELECT trainer_email FROM call_days WHERE id = ?").bind(body.id).first();
    if (!row) return json({ error: "Nie ma takiego terminu" }, 404);
    if (!who.admin && String(row.trainer_email).toLowerCase() !== who.email) return json({ error: "Ten termin wyznaczy\u0142 inny trener" }, 403);
    const called = body.called === true ? 1 : body.called === false ? 0 : null;
    await env.DB.prepare("UPDATE call_days SET called = ?, called_at = ? WHERE id = ?").bind(called, called === null ? null : Math.floor(Date.now() / 1e3), body.id).run();
    return json({ ok: true, called });
  }
  if (action === "delete") {
    const row = await env.DB.prepare("SELECT * FROM call_days WHERE id = ?").bind(body.id).first();
    if (!row) return json({ ok: true });
    if (!who.admin && String(row.trainer_email).toLowerCase() !== who.email) return json({ error: "Ten termin wyznaczy\u0142 inny trener" }, 403);
    try {
      await gcalDelete(env, row.gcal_event_id);
    } catch (e) {
    }
    await env.DB.prepare("DELETE FROM call_days WHERE id = ?").bind(body.id).run();
    return json({ ok: true });
  }
  return json({ error: "Nieznana akcja" }, 404);
}
__name(handleCallDaysAction, "handleCallDaysAction");
__name2(handleCallDaysAction, "handleCallDaysAction");
async function handleMyCallDays(request, env, url) {
  const nsToken = url.searchParams.get("ns_token"), nsEmail = url.searchParams.get("ns_email"), nsServer = url.searchParams.get("ns_server");
  const valid = await validateNsToken(nsToken, nsEmail, nsServer);
  if (!valid) return json({ error: "Nieprawid\u0142owy lub wygas\u0142y token NS \u2014 zaloguj si\u0119 ponownie" }, 401);
  const u = await nsUserByEmail(env, nsEmail);
  if (!u) return json({ ok: true, days: [] });
  await ensureCallDaysTable(env);
  const from = new Date(Date.now() - 14 * 864e5).toISOString().slice(0, 10);
  const rows = (await env.DB.prepare("SELECT id, day, time, note, trainer_name, trainer_phone, created_at FROM call_days WHERE ns_student_id = ? AND day >= ? ORDER BY day, time").bind(String(u.id), from).all()).results || [];
  return json({ ok: true, days: rows });
}
__name(handleMyCallDays, "handleMyCallDays");
__name2(handleMyCallDays, "handleMyCallDays");
async function handleCallDaysIcs(request, env, url) {
  if (!env.CALL_DAYS_ICS_KEY || url.searchParams.get("key") !== env.CALL_DAYS_ICS_KEY) return new Response("Forbidden", { status: 403 });
  await ensureCallDaysTable(env);
  const from = new Date(Date.now() - 30 * 864e5).toISOString().slice(0, 10);
  const rows = (await env.DB.prepare("SELECT * FROM call_days WHERE day >= ? ORDER BY day, time").bind(from).all()).results || [];
  const esc = /* @__PURE__ */ __name((t) => String(t || "").replace(/\\/g, "\\\\").replace(/;/g, "\\;").replace(/,/g, "\\,").replace(/\n/g, "\\n"), "esc");
  const stamp = (/* @__PURE__ */ new Date()).toISOString().replace(/[-:]/g, "").slice(0, 15) + "Z";
  const lines = ["BEGIN:VCALENDAR", "VERSION:2.0", "PRODID:-//NewSpeech//Call days//PL", "X-WR-CALNAME:Telefony od kursant\xF3w", "X-WR-TIMEZONE:Europe/Warsaw"];
  for (const r of rows) {
    const d = r.day.replace(/-/g, "");
    lines.push("BEGIN:VEVENT", "UID:callday-" + r.id + "@newspeech", "DTSTAMP:" + stamp);
    if (r.time && /^\d{1,2}:\d{2}$/.test(r.time)) {
      const [h, m] = r.time.split(":").map(Number), e = h * 60 + m + 15, p2 = /* @__PURE__ */ __name((n) => String(n).padStart(2, "0"), "p2");
      lines.push("DTSTART;TZID=Europe/Warsaw:" + d + "T" + p2(h) + p2(m) + "00", "DTEND;TZID=Europe/Warsaw:" + d + "T" + p2(Math.floor(e / 60) % 24) + p2(e % 60) + "00");
    } else {
      const nd = /* @__PURE__ */ new Date(r.day + "T00:00:00Z");
      nd.setUTCDate(nd.getUTCDate() + 1);
      lines.push("DTSTART;VALUE=DATE:" + d, "DTEND;VALUE=DATE:" + nd.toISOString().slice(0, 10).replace(/-/g, ""));
    }
    lines.push("SUMMARY:" + esc("\u{1F4DE} Telefon od kursanta: " + (r.student_name || r.ns_student_id)), "DESCRIPTION:" + esc((r.note ? "Notatka: " + r.note + "\n" : "") + "Wyznaczy\u0142(a): " + (r.trainer_name || r.trainer_email)), "END:VEVENT");
  }
  lines.push("END:VCALENDAR");
  return new Response(lines.join("\r\n"), { headers: { "Content-Type": "text/calendar; charset=utf-8", "Cache-Control": "no-store" } });
}
__name(handleCallDaysIcs, "handleCallDaysIcs");
__name2(handleCallDaysIcs, "handleCallDaysIcs");
async function handleMyContactList(request, env, url) {
  const nsToken = url.searchParams.get("ns_token"), nsEmail = url.searchParams.get("ns_email"), nsServer = url.searchParams.get("ns_server");
  const valid = await validateNsToken(nsToken, nsEmail, nsServer);
  if (!valid) return json({ error: "Nieprawid\u0142owy lub wygas\u0142y token NS \u2014 zaloguj si\u0119 ponownie" }, 401);
  await ensureContactTables(env);
  const pgClient = new Client({ connectionString: env.HYPERDRIVE.connectionString });
  let me = null, visits = [];
  try {
    await pgClient.connect();
    const u = await pgClient.query(`SELECT id FROM users WHERE LOWER(email) = LOWER($1) AND deleted_at IS NULL LIMIT 1`, [nsEmail]);
    me = u.rows[0] ? String(u.rows[0].id) : null;
    if (me) visits = (await pgClient.query(`SELECT id, date FROM visit_dates WHERE student_id = $1 ORDER BY date DESC LIMIT 50`, [me])).rows;
  } catch (e) {
    return json({ error: "B\u0142\u0105d bazy NS: " + e.message }, 500);
  } finally {
    try {
      await pgClient.end();
    } catch (e) {
    }
  }
  if (!me) return json({ ok: true, list: null, lists: [] });
  const att = (await env.DB.prepare("SELECT visit_id, list_id, updated_at FROM harmonogram_contact_lists WHERE ns_student_id = ?").bind(me).all()).results || [];
  if (!att.length) return json({ ok: true, list: null, lists: [] });
  const vDate = {};
  visits.forEach((v) => {
    vDate[String(v.id)] = v.date ? new Date(v.date).toISOString().slice(0, 10) : "";
  });
  const today = (/* @__PURE__ */ new Date()).toISOString().slice(0, 10);
  att.sort((a, b) => {
    const da = vDate[String(a.visit_id)] || "", db = vDate[String(b.visit_id)] || "";
    const fa = da >= today, fb = db >= today;
    if (fa !== fb) return fa ? -1 : 1;
    return fa ? da < db ? -1 : da > db ? 1 : 0 : da > db ? -1 : da < db ? 1 : (b.updated_at || 0) - (a.updated_at || 0);
  });
  const ids = [...new Set(att.map((a) => a.list_id))];
  const ph = ids.map(() => "?").join(",");
  const lists = (await env.DB.prepare(`SELECT id, name FROM contact_lists WHERE id IN (${ph})`).bind(...ids).all()).results || [];
  const members = (await env.DB.prepare(
    `SELECT m.list_id, m.ns_student_id, m.name, m.phone, m.note FROM contact_list_members m
     JOIN student_phone_consent c ON c.ns_student_id = m.ns_student_id AND c.consent = 1
     WHERE m.list_id IN (${ph}) ORDER BY m.name`
  ).bind(...ids).all()).results || [];
  const out = att.map((a) => {
    const l = lists.find((x) => x.id === a.list_id);
    if (!l) return null;
    const vd = vDate[String(a.visit_id)] || null;
    const prev = vd ? visits.map((v) => vDate[String(v.id)]).find((d) => d && d < vd) || null : null;
    return {
      id: l.id,
      name: l.name,
      visit_id: String(a.visit_id),
      visit_date: vd,
      period_from: prev,
      goal: 6,
      members: members.filter((m) => m.list_id === l.id && String(m.ns_student_id) !== me).map((m) => ({ ns_student_id: String(m.ns_student_id), name: m.name, phone: m.phone, note: m.note || "" }))
    };
  }).filter(Boolean);
  return json({ ok: true, list: out[0] || null, lists: out });
}
__name(handleMyContactList, "handleMyContactList");
__name2(handleMyContactList, "handleMyContactList");
async function contactListAllowed(env, who, listId) {
  const row = await env.DB.prepare("SELECT owner_email FROM contact_lists WHERE id = ?").bind(listId).first();
  if (!row) return null;
  if (!who.admin && String(row.owner_email).toLowerCase() !== who.email) return false;
  return true;
}
__name(contactListAllowed, "contactListAllowed");
async function handleContactLists(request, env, url) {
  const who = await contactAuth(env, url.searchParams.get("ns_token"), url.searchParams.get("ns_email"), url.searchParams.get("ns_server"));
  if (!who) return json({ error: "Nieprawid\u0142owy lub wygas\u0142y token NS \u2014 zaloguj si\u0119 ponownie" }, 401);
  if (who.forbidden) return json({ error: "Listy kontakt\xF3w prowadzi trener" }, 403);
  await ensureContactTables(env);
  const listsRes = who.admin ? await env.DB.prepare("SELECT * FROM contact_lists ORDER BY name").all() : await env.DB.prepare("SELECT * FROM contact_lists WHERE LOWER(owner_email) = ? ORDER BY name").bind(who.email).all();
  const lists = listsRes.results || [];
  const ids = lists.map((l) => l.id);
  let members = [], attachments = [];
  if (ids.length) {
    const ph = ids.map(() => "?").join(",");
    members = (await env.DB.prepare(`SELECT list_id, ns_student_id, name, phone, note FROM contact_list_members WHERE list_id IN (${ph}) ORDER BY name`).bind(...ids).all()).results || [];
    attachments = (await env.DB.prepare(`SELECT visit_id, list_id, ns_student_id FROM harmonogram_contact_lists WHERE list_id IN (${ph})`).bind(...ids).all()).results || [];
  }
  const consents = (await env.DB.prepare("SELECT ns_student_id FROM student_phone_consent WHERE consent = 1").all()).results || [];
  return json({
    ok: true,
    is_admin: who.admin,
    lists: lists.map((l) => ({
      id: l.id,
      name: l.name,
      owner_email: l.owner_email,
      members: members.filter((m) => m.list_id === l.id),
      attachments: attachments.filter((a) => a.list_id === l.id)
    })),
    consents
  });
}
__name(handleContactLists, "handleContactLists");
async function handleContactListsAction(request, env, action) {
  let body = {};
  try {
    body = await request.json();
  } catch (e) {
  }
  const who = await contactAuth(env, body.ns_token, body.ns_email, body.ns_server);
  if (!who) return json({ error: "Nieprawid\u0142owy lub wygas\u0142y token NS \u2014 zaloguj si\u0119 ponownie" }, 401);
  if (who.forbidden) return json({ error: "Listy kontakt\xF3w prowadzi trener" }, 403);
  await ensureContactTables(env);
  const now = Math.floor(Date.now() / 1e3);
  if (action === "save") {
    const name = String(body.name || "").trim();
    if (!name) return json({ error: "Podaj nazw\u0119 listy" }, 400);
    if (body.id) {
      const ok = await contactListAllowed(env, who, body.id);
      if (ok === null) return json({ error: "Nie ma takiej listy" }, 404);
      if (!ok) return json({ error: "To nie jest Twoja lista" }, 403);
      await env.DB.prepare("UPDATE contact_lists SET name = ?, updated_at = ? WHERE id = ?").bind(name, now, body.id).run();
      return json({ ok: true, id: body.id });
    }
    const r = await env.DB.prepare("INSERT INTO contact_lists (name, owner_email, created_at, updated_at) VALUES (?, ?, ?, ?)").bind(name, who.email, now, now).run();
    return json({ ok: true, id: r.meta.last_row_id });
  }
  if (action === "delete") {
    const ok = await contactListAllowed(env, who, body.id);
    if (ok === null) return json({ ok: true });
    if (!ok) return json({ error: "To nie jest Twoja lista" }, 403);
    await env.DB.batch([
      env.DB.prepare("DELETE FROM contact_list_members WHERE list_id = ?").bind(body.id),
      env.DB.prepare("DELETE FROM harmonogram_contact_lists WHERE list_id = ?").bind(body.id),
      env.DB.prepare("DELETE FROM contact_lists WHERE id = ?").bind(body.id)
    ]);
    return json({ ok: true });
  }
  if (action === "member-add") {
    const ok = await contactListAllowed(env, who, body.list_id);
    if (ok === null) return json({ error: "Nie ma takiej listy" }, 404);
    if (!ok) return json({ error: "To nie jest Twoja lista" }, 403);
    const phone = String(body.phone || "").trim();
    const name = String(body.name || "").trim();
    if (!body.ns_student_id || !name) return json({ error: "Brak kursanta" }, 400);
    if (!phone) return json({ error: "Brak numeru telefonu" }, 400);
    if (!body.consent) return json({ error: "Brak potwierdzenia zgody kursanta na udost\u0119pnienie numeru" }, 400);
    await env.DB.batch([
      env.DB.prepare(`INSERT INTO student_phone_consent (ns_student_id, name, phone, consent, confirmed_by, confirmed_at) VALUES (?, ?, ?, 1, ?, ?)
        ON CONFLICT(ns_student_id) DO UPDATE SET name = excluded.name, phone = excluded.phone, consent = 1, confirmed_by = excluded.confirmed_by, confirmed_at = excluded.confirmed_at`).bind(body.ns_student_id, name, phone, who.email, now),
      env.DB.prepare(`INSERT INTO contact_list_members (list_id, ns_student_id, name, phone, note, added_by, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT(list_id, ns_student_id) DO UPDATE SET name = excluded.name, phone = excluded.phone, note = excluded.note`).bind(body.list_id, body.ns_student_id, name, phone, String(body.note || "").trim() || null, who.email, now),
      env.DB.prepare("UPDATE contact_lists SET updated_at = ? WHERE id = ?").bind(now, body.list_id)
    ]);
    return json({ ok: true });
  }
  if (action === "member-remove") {
    const ok = await contactListAllowed(env, who, body.list_id);
    if (ok === null) return json({ error: "Nie ma takiej listy" }, 404);
    if (!ok) return json({ error: "To nie jest Twoja lista" }, 403);
    await env.DB.batch([
      env.DB.prepare("DELETE FROM contact_list_members WHERE list_id = ? AND ns_student_id = ?").bind(body.list_id, body.ns_student_id),
      env.DB.prepare("UPDATE contact_lists SET updated_at = ? WHERE id = ?").bind(now, body.list_id)
    ]);
    return json({ ok: true });
  }
  if (action === "attach") {
    if (!body.visit_id || !body.ns_student_id) return json({ error: "Brak harmonogramu" }, 400);
    if (!body.list_id) {
      const cur = await env.DB.prepare("SELECT list_id FROM harmonogram_contact_lists WHERE visit_id = ?").bind(String(body.visit_id)).first();
      if (cur) {
        const own = await contactListAllowed(env, who, cur.list_id);
        if (own === false) return json({ error: "To nie jest Twoja lista" }, 403);
      }
      await env.DB.prepare("DELETE FROM harmonogram_contact_lists WHERE visit_id = ?").bind(String(body.visit_id)).run();
      return json({ ok: true });
    }
    const ok = await contactListAllowed(env, who, body.list_id);
    if (ok === null) return json({ error: "Nie ma takiej listy" }, 404);
    if (!ok) return json({ error: "To nie jest Twoja lista" }, 403);
    await env.DB.prepare(`INSERT INTO harmonogram_contact_lists (visit_id, list_id, ns_student_id, attached_by, updated_at) VALUES (?, ?, ?, ?, ?)
      ON CONFLICT(visit_id) DO UPDATE SET list_id = excluded.list_id, ns_student_id = excluded.ns_student_id, attached_by = excluded.attached_by, updated_at = excluded.updated_at`).bind(String(body.visit_id), body.list_id, body.ns_student_id, who.email, now).run();
    return json({ ok: true });
  }
  return json({ error: "Nieznana akcja" }, 404);
}
__name(handleContactListsAction, "handleContactListsAction");
__name2(handleContactLists, "handleContactLists");
__name2(handleContactListsAction, "handleContactListsAction");
async function handleTrainerPermissionsMine(request, env, url) {
  const nsToken = url.searchParams.get("ns_token");
  const nsEmail = url.searchParams.get("ns_email");
  const nsServer = url.searchParams.get("ns_server");
  const valid = await validateNsToken(nsToken, nsEmail, nsServer);
  if (!valid) return json({ error: "Nieprawid\u0142owy lub wygas\u0142y token NS \u2014 zaloguj si\u0119 ponownie" }, 401);
  const keys = ["can_view_recordings", "can_listen_recordings", "can_add_reviews", "can_add_voice_reviews", "can_manage_students"];
  const defaults2 = { can_view_recordings: true, can_listen_recordings: true, can_add_reviews: true, can_add_voice_reviews: true, can_manage_students: true };
  try {
    const row = await env.DB.prepare("SELECT * FROM trainer_permissions WHERE trainer_email = ?").bind(nsEmail).first();
    if (!row) return json({ ok: true, permissions: defaults2, rating_scale: "numbers" });
    const perms = {};
    keys.forEach(function(k) {
      perms[k] = !!row[k];
    });
    return json({ ok: true, permissions: perms, rating_scale: row.rating_scale === "words" ? "words" : "numbers" });
  } catch (e) {
    return json({ ok: true, permissions: defaults2, rating_scale: "numbers" });
  }
}
__name(handleTrainerPermissionsMine, "handleTrainerPermissionsMine");
__name2(handleTrainerPermissionsMine, "handleTrainerPermissionsMine");
async function handleDiskStatus(request, env, url) {
  const nsToken = url.searchParams.get("ns_token");
  const nsEmail = url.searchParams.get("ns_email");
  const nsServer = url.searchParams.get("ns_server");
  const valid = await validateNsToken(nsToken, nsEmail, nsServer);
  if (!valid) return json({ error: "Nieprawid\u0142owy lub wygas\u0142y token NS \u2014 zaloguj si\u0119 ponownie" }, 401);
  const admin = await isNsAdmin(env, nsEmail);
  if (!admin) return json({ error: "Tylko admin mo\u017Ce przegl\u0105da\u0107 stan serwera" }, 403);
  const base = nsServerBase(nsServer);
  let resp;
  try {
    resp = await fetch(base + "/disk-status.json");
  } catch (e) {
    return json({ error: "Nie uda\u0142o si\u0119 po\u0142\u0105czy\u0107 z serwerem NS" }, 502);
  }
  if (!resp.ok) return json({ error: "Serwer NS nie zwr\xF3ci\u0142 statusu dysku (skrypt cron m\xF3g\u0142 jeszcze nie zapisa\u0107 pliku)" }, 502);
  const data = await resp.json().catch(() => null);
  if (!data) return json({ error: "Nieprawid\u0142owa odpowied\u017A z serwera NS" }, 502);
  return json({ ok: true, disk: data });
}
__name(handleDiskStatus, "handleDiskStatus");
__name2(handleDiskStatus, "handleDiskStatus");
async function handleTrainerPermissionsList(request, env, url) {
  const nsToken = url.searchParams.get("ns_token");
  const nsEmail = url.searchParams.get("ns_email");
  const nsServer = url.searchParams.get("ns_server");
  const valid = await validateNsToken(nsToken, nsEmail, nsServer);
  if (!valid) return json({ error: "Nieprawid\u0142owy lub wygas\u0142y token NS \u2014 zaloguj si\u0119 ponownie" }, 401);
  const admin = await isNsAdmin(env, nsEmail);
  if (!admin) return json({ error: "Tylko admin mo\u017Ce przegl\u0105da\u0107 uprawnienia trener\xF3w" }, 403);
  const { results } = await env.DB.prepare("SELECT * FROM trainer_permissions").all();
  return json({ ok: true, permissions: results });
}
__name(handleTrainerPermissionsList, "handleTrainerPermissionsList");
__name2(handleTrainerPermissionsList, "handleTrainerPermissionsList");
async function handleTrainerPermissionsSave(request, env) {
  const body = await request.json();
  const {
    ns_token,
    ns_email,
    ns_server,
    trainer_email,
    can_view_recordings,
    can_listen_recordings,
    can_add_reviews,
    can_add_voice_reviews,
    can_manage_students,
    rating_scale
  } = body;
  if (!trainer_email) return json({ error: "Brak trainer_email" }, 400);
  const valid = await validateNsToken(ns_token, ns_email, ns_server);
  if (!valid) return json({ error: "Nieprawid\u0142owy lub wygas\u0142y token NS \u2014 zaloguj si\u0119 ponownie" }, 401);
  const admin = await isNsAdmin(env, ns_email);
  if (!admin) return json({ error: "Tylko admin mo\u017Ce zmienia\u0107 uprawnienia trener\xF3w" }, 403);
  await env.DB.prepare(
    `INSERT INTO trainer_permissions
       (trainer_email, can_view_recordings, can_listen_recordings, can_add_reviews, can_add_voice_reviews, can_manage_students, rating_scale, updated_at)
     VALUES (?, ?, ?, ?, ?, ?, ?, ?)
     ON CONFLICT(trainer_email) DO UPDATE SET
       rating_scale = excluded.rating_scale,
       can_view_recordings = excluded.can_view_recordings,
       can_listen_recordings = excluded.can_listen_recordings,
       can_add_reviews = excluded.can_add_reviews,
       can_add_voice_reviews = excluded.can_add_voice_reviews,
       can_manage_students = excluded.can_manage_students,
       updated_at = excluded.updated_at`
  ).bind(
    trainer_email,
    can_view_recordings ? 1 : 0,
    can_listen_recordings ? 1 : 0,
    can_add_reviews ? 1 : 0,
    can_add_voice_reviews ? 1 : 0,
    can_manage_students ? 1 : 0,
    rating_scale === "words" ? "words" : "numbers",
    Math.floor(Date.now() / 1e3)
  ).run();
  return json({ ok: true });
}
__name(handleTrainerPermissionsSave, "handleTrainerPermissionsSave");
__name2(handleTrainerPermissionsSave, "handleTrainerPermissionsSave");
async function handleHarmonogramCacheList(request, env, url) {
  const nsToken = url.searchParams.get("ns_token");
  const nsEmail = url.searchParams.get("ns_email");
  const nsServer = url.searchParams.get("ns_server");
  const studentId = url.searchParams.get("student_id");
  const valid = await validateNsToken(nsToken, nsEmail, nsServer);
  if (!valid) return json({ error: "Nieprawid\u0142owy token" }, 401);
  if (!studentId) return json({ error: "Brak student_id" }, 400);
  const { results } = await env.DB.prepare(
    "SELECT period_id, fingerprint, period_pct, rows_json FROM harmonogram_closed_cache WHERE student_id = ?"
  ).bind(studentId).all();
  return json({ ok: true, entries: results });
}
__name(handleHarmonogramCacheList, "handleHarmonogramCacheList");
__name2(handleHarmonogramCacheList, "handleHarmonogramCacheList");
async function handleHarmonogramCacheSave(request, env) {
  const body = await request.json();
  const { ns_token, ns_email, ns_server, entries } = body;
  const valid = await validateNsToken(ns_token, ns_email, ns_server);
  if (!valid) return json({ error: "Nieprawid\u0142owy token" }, 401);
  if (!Array.isArray(entries) || !entries.length) return json({ error: "Brak danych do zapisania" }, 400);
  const now = Math.floor(Date.now() / 1e3);
  for (const e of entries) {
    await env.DB.prepare(
      `INSERT INTO harmonogram_closed_cache (period_id, student_id, fingerprint, period_pct, rows_json, computed_at)
       VALUES (?, ?, ?, ?, ?, ?)
       ON CONFLICT(period_id) DO UPDATE SET fingerprint=excluded.fingerprint, period_pct=excluded.period_pct,
         rows_json=excluded.rows_json, computed_at=excluded.computed_at`
    ).bind(e.period_id, e.student_id, e.fingerprint, e.period_pct, e.rows_json, now).run();
  }
  return json({ ok: true, saved: entries.length });
}
__name(handleHarmonogramCacheSave, "handleHarmonogramCacheSave");
__name2(handleHarmonogramCacheSave, "handleHarmonogramCacheSave");
async function handleScheduleCacheList(request, env, url) {
  const nsToken = url.searchParams.get("ns_token");
  const nsEmail = url.searchParams.get("ns_email");
  const nsServer = url.searchParams.get("ns_server");
  const valid = await validateNsToken(nsToken, nsEmail, nsServer);
  if (!valid) return json({ error: "Nieprawid\u0142owy token" }, 401);
  const { results } = await env.DB.prepare(
    "SELECT student_id, student_name, pct, period_from, period_to, computed_at FROM schedule_pct_cache"
  ).all();
  return json({ ok: true, entries: results });
}
__name(handleScheduleCacheList, "handleScheduleCacheList");
__name2(handleScheduleCacheList, "handleScheduleCacheList");
async function handleScheduleCacheSave(request, env) {
  const body = await request.json();
  const { ns_token, ns_email, ns_server, entries } = body;
  const valid = await validateNsToken(ns_token, ns_email, ns_server);
  if (!valid) return json({ error: "Nieprawid\u0142owy token" }, 401);
  if (!Array.isArray(entries) || !entries.length) return json({ error: "Brak danych do zapisania" }, 400);
  const now = Math.floor(Date.now() / 1e3);
  for (const e of entries) {
    await env.DB.prepare(
      `INSERT INTO schedule_pct_cache (student_id, student_name, pct, period_from, period_to, computed_at)
       VALUES (?, ?, ?, ?, ?, ?)
       ON CONFLICT(student_id) DO UPDATE SET student_name=excluded.student_name, pct=excluded.pct,
         period_from=excluded.period_from, period_to=excluded.period_to, computed_at=excluded.computed_at`
    ).bind(e.student_id, e.student_name || "", e.pct, e.period_from || "", e.period_to || "", now).run();
  }
  return json({ ok: true, saved: entries.length });
}
__name(handleScheduleCacheSave, "handleScheduleCacheSave");
__name2(handleScheduleCacheSave, "handleScheduleCacheSave");
async function handleVoiceReviewUpload(request, env, url) {
  const recordId = url.searchParams.get("record_id");
  const studentId = url.searchParams.get("student_id") || "";
  const nsToken = url.searchParams.get("ns_token");
  const nsEmail = url.searchParams.get("ns_email");
  const nsServer = url.searchParams.get("ns_server");
  if (!recordId) return json({ error: "Brak record_id" }, 400);
  const valid = await validateNsToken(nsToken, nsEmail, nsServer);
  if (!valid) return json({ error: "Nieprawid\u0142owy lub wygas\u0142y token NS \u2014 zaloguj si\u0119 ponownie" }, 401);
  const allowedVoice = await hasTrainerPermission(env, nsEmail, "can_add_voice_reviews");
  if (!allowedVoice) return json({ error: "Brak uprawnie\u0144 do dodawania recenzji g\u0142osowych" }, 403);
  const contentType = request.headers.get("Content-Type") || "audio/webm";
  const audioData = await request.arrayBuffer();
  if (audioData.byteLength === 0) return json({ error: "Puste nagranie" }, 400);
  const r2Key = `voice-reviews/${recordId}.webm`;
  await env.RECORDINGS.put(r2Key, audioData, { httpMetadata: { contentType } });
  await env.DB.prepare(
    `INSERT INTO voice_reviews (record_id, student_id, trainer_email, r2_key, created_at)
     VALUES (?, ?, ?, ?, ?)
     ON CONFLICT(record_id) DO UPDATE SET r2_key=excluded.r2_key, trainer_email=excluded.trainer_email, created_at=excluded.created_at`
  ).bind(recordId, studentId, nsEmail, r2Key, Math.floor(Date.now() / 1e3)).run();
  return json({ ok: true });
}
__name(handleVoiceReviewUpload, "handleVoiceReviewUpload");
__name2(handleVoiceReviewUpload, "handleVoiceReviewUpload");
async function handleVoiceReviewAudio(request, env, url) {
  const recordId = url.searchParams.get("record_id");
  const nsToken = url.searchParams.get("ns_token");
  const nsEmail = url.searchParams.get("ns_email");
  const nsServer = url.searchParams.get("ns_server");
  const valid = await validateNsToken(nsToken, nsEmail, nsServer);
  if (!valid) return new Response("Nieprawid\u0142owy token", { status: 401, headers: CORS });
  const row = await env.DB.prepare("SELECT r2_key FROM voice_reviews WHERE record_id = ?").bind(recordId).first();
  if (!row) return new Response("Brak recenzji g\u0142osowej", { status: 404, headers: CORS });
  const obj = await env.RECORDINGS.get(row.r2_key);
  if (!obj) return new Response("Plik nie istnieje w magazynie", { status: 404, headers: CORS });
  const headers = new Headers(CORS);
  headers.set("Content-Type", obj.httpMetadata && obj.httpMetadata.contentType || "audio/webm");
  return new Response(obj.body, { headers });
}
__name(handleVoiceReviewAudio, "handleVoiceReviewAudio");
__name2(handleVoiceReviewAudio, "handleVoiceReviewAudio");
async function handleVoiceReviewCheck(request, env, url) {
  const idsParam = url.searchParams.get("record_ids") || "";
  const nsToken = url.searchParams.get("ns_token");
  const nsEmail = url.searchParams.get("ns_email");
  const nsServer = url.searchParams.get("ns_server");
  const valid = await validateNsToken(nsToken, nsEmail, nsServer);
  if (!valid) return json({ error: "Nieprawid\u0142owy token" }, 401);
  const ids = idsParam.split(",").map((s) => s.trim()).filter(Boolean);
  if (!ids.length) return json({ has_review: [] });
  const placeholders = ids.map(() => "?").join(",");
  const { results } = await env.DB.prepare(
    `SELECT record_id FROM voice_reviews WHERE record_id IN (${placeholders})`
  ).bind(...ids).all();
  return json({ has_review: results.map((r) => r.record_id) });
}
__name(handleVoiceReviewCheck, "handleVoiceReviewCheck");
__name2(handleVoiceReviewCheck, "handleVoiceReviewCheck");
async function handleVoiceReviewDelete(request, env, url) {
  const recordId = url.searchParams.get("record_id");
  const nsToken = url.searchParams.get("ns_token");
  const nsEmail = url.searchParams.get("ns_email");
  const nsServer = url.searchParams.get("ns_server");
  const valid = await validateNsToken(nsToken, nsEmail, nsServer);
  if (!valid) return json({ error: "Nieprawid\u0142owy token" }, 401);
  const allowedVoice = await hasTrainerPermission(env, nsEmail, "can_add_voice_reviews");
  if (!allowedVoice) return json({ error: "Brak uprawnie\u0144 do zarz\u0105dzania recenzjami g\u0142osowymi" }, 403);
  const row = await env.DB.prepare("SELECT r2_key FROM voice_reviews WHERE record_id = ?").bind(recordId).first();
  if (row) {
    await env.RECORDINGS.delete(row.r2_key);
    await env.DB.prepare("DELETE FROM voice_reviews WHERE record_id = ?").bind(recordId).run();
  }
  return json({ ok: true });
}
__name(handleVoiceReviewDelete, "handleVoiceReviewDelete");
__name2(handleVoiceReviewDelete, "handleVoiceReviewDelete");
async function handleDiaryReplyGet(request, env, url) {
  const studentId = url.searchParams.get("student_id");
  const entryDate = url.searchParams.get("entry_date");
  const nsToken = url.searchParams.get("ns_token");
  const nsEmail = url.searchParams.get("ns_email");
  const nsServer = url.searchParams.get("ns_server");
  if (!studentId || !entryDate) return json({ error: "Brak student_id lub entry_date" }, 400);
  const valid = await validateNsToken(nsToken, nsEmail, nsServer);
  if (!valid) return json({ error: "Nieprawid\u0142owy lub wygas\u0142y token NS" }, 401);
  const row = await env.DB.prepare(
    "SELECT text_reply, audio_r2_key, trainer_email, updated_at FROM diary_replies WHERE student_id = ? AND entry_date = ?"
  ).bind(studentId, entryDate).first();
  return json({
    ok: true,
    text_reply: row ? row.text_reply : null,
    has_audio: !!(row && row.audio_r2_key),
    trainer_email: row ? row.trainer_email : null,
    updated_at: row ? row.updated_at : null
  });
}
__name(handleDiaryReplyGet, "handleDiaryReplyGet");
__name2(handleDiaryReplyGet, "handleDiaryReplyGet");
async function handleDiaryReplySaveText(request, env) {
  const body = await request.json();
  const { ns_token, ns_email, ns_server, student_id, entry_date, text_reply } = body;
  if (!student_id || !entry_date) return json({ error: "Brak student_id lub entry_date" }, 400);
  const valid = await validateNsToken(ns_token, ns_email, ns_server);
  if (!valid) return json({ error: "Nieprawid\u0142owy lub wygas\u0142y token NS" }, 401);
  const allowed = await hasTrainerPermission(env, ns_email, "can_add_reviews");
  if (!allowed) return json({ error: "Brak uprawnie\u0144 do dodawania odpowiedzi" }, 403);
  await env.DB.prepare(
    `INSERT INTO diary_replies (student_id, entry_date, trainer_email, text_reply, updated_at)
     VALUES (?, ?, ?, ?, ?)
     ON CONFLICT(student_id, entry_date) DO UPDATE SET
       text_reply = excluded.text_reply, trainer_email = excluded.trainer_email, updated_at = excluded.updated_at`
  ).bind(student_id, entry_date, ns_email, text_reply || null, Math.floor(Date.now() / 1e3)).run();
  return json({ ok: true });
}
__name(handleDiaryReplySaveText, "handleDiaryReplySaveText");
__name2(handleDiaryReplySaveText, "handleDiaryReplySaveText");
async function handleDiaryReplySaveAudio(request, env, url) {
  const studentId = url.searchParams.get("student_id");
  const entryDate = url.searchParams.get("entry_date");
  const nsToken = url.searchParams.get("ns_token");
  const nsEmail = url.searchParams.get("ns_email");
  const nsServer = url.searchParams.get("ns_server");
  if (!studentId || !entryDate) return json({ error: "Brak student_id lub entry_date" }, 400);
  const valid = await validateNsToken(nsToken, nsEmail, nsServer);
  if (!valid) return json({ error: "Nieprawid\u0142owy lub wygas\u0142y token NS" }, 401);
  const allowedVoice = await hasTrainerPermission(env, nsEmail, "can_add_voice_reviews");
  if (!allowedVoice) return json({ error: "Brak uprawnie\u0144 do dodawania odpowiedzi g\u0142osowych" }, 403);
  const contentType = request.headers.get("Content-Type") || "audio/webm";
  const audioData = await request.arrayBuffer();
  if (audioData.byteLength === 0) return json({ error: "Puste nagranie" }, 400);
  const r2Key = `diary-replies/${studentId}/${entryDate}.webm`;
  await env.RECORDINGS.put(r2Key, audioData, { httpMetadata: { contentType } });
  await env.DB.prepare(
    `INSERT INTO diary_replies (student_id, entry_date, trainer_email, audio_r2_key, updated_at)
     VALUES (?, ?, ?, ?, ?)
     ON CONFLICT(student_id, entry_date) DO UPDATE SET
       audio_r2_key = excluded.audio_r2_key, trainer_email = excluded.trainer_email, updated_at = excluded.updated_at`
  ).bind(studentId, entryDate, nsEmail, r2Key, Math.floor(Date.now() / 1e3)).run();
  return json({ ok: true });
}
__name(handleDiaryReplySaveAudio, "handleDiaryReplySaveAudio");
__name2(handleDiaryReplySaveAudio, "handleDiaryReplySaveAudio");
async function handleDiaryReplyAudio(request, env, url) {
  const studentId = url.searchParams.get("student_id");
  const entryDate = url.searchParams.get("entry_date");
  const nsToken = url.searchParams.get("ns_token");
  const nsEmail = url.searchParams.get("ns_email");
  const nsServer = url.searchParams.get("ns_server");
  const valid = await validateNsToken(nsToken, nsEmail, nsServer);
  if (!valid) return new Response("Nieprawid\u0142owy token", { status: 401, headers: CORS });
  const row = await env.DB.prepare(
    "SELECT audio_r2_key FROM diary_replies WHERE student_id = ? AND entry_date = ?"
  ).bind(studentId, entryDate).first();
  if (!row || !row.audio_r2_key) return new Response("Brak nagrania", { status: 404, headers: CORS });
  const obj = await env.RECORDINGS.get(row.audio_r2_key);
  if (!obj) return new Response("Plik nie istnieje w magazynie", { status: 404, headers: CORS });
  const headers = new Headers(CORS);
  headers.set("Content-Type", obj.httpMetadata && obj.httpMetadata.contentType || "audio/webm");
  return new Response(obj.body, { headers });
}
__name(handleDiaryReplyAudio, "handleDiaryReplyAudio");
__name2(handleDiaryReplyAudio, "handleDiaryReplyAudio");
async function handleDiaryReplyDeleteAudio(request, env, url) {
  const studentId = url.searchParams.get("student_id");
  const entryDate = url.searchParams.get("entry_date");
  const nsToken = url.searchParams.get("ns_token");
  const nsEmail = url.searchParams.get("ns_email");
  const nsServer = url.searchParams.get("ns_server");
  const valid = await validateNsToken(nsToken, nsEmail, nsServer);
  if (!valid) return json({ error: "Nieprawid\u0142owy token" }, 401);
  const allowedVoice = await hasTrainerPermission(env, nsEmail, "can_add_voice_reviews");
  if (!allowedVoice) return json({ error: "Brak uprawnie\u0144 do zarz\u0105dzania odpowiedziami g\u0142osowymi" }, 403);
  const row = await env.DB.prepare(
    "SELECT audio_r2_key FROM diary_replies WHERE student_id = ? AND entry_date = ?"
  ).bind(studentId, entryDate).first();
  if (row && row.audio_r2_key) {
    await env.RECORDINGS.delete(row.audio_r2_key);
    await env.DB.prepare(
      "UPDATE diary_replies SET audio_r2_key = NULL WHERE student_id = ? AND entry_date = ?"
    ).bind(studentId, entryDate).run();
  }
  return json({ ok: true });
}
__name(handleDiaryReplyDeleteAudio, "handleDiaryReplyDeleteAudio");
__name2(handleDiaryReplyDeleteAudio, "handleDiaryReplyDeleteAudio");
async function handleFavoriteToggle(request, env) {
  const body = await request.json();
  const { ns_token, ns_email, ns_server, record_id, student_id, student_name, category_name, record_date, ns_path } = body;
  if (!record_id) return json({ error: "Brak record_id" }, 400);
  const valid = await validateNsToken(ns_token, ns_email, ns_server);
  if (!valid) return json({ error: "Nieprawid\u0142owy lub wygas\u0142y token NS \u2014 zaloguj si\u0119 ponownie" }, 401);
  const existing = await env.DB.prepare(
    "SELECT id FROM favorite_records WHERE record_id = ? AND created_by = ?"
  ).bind(record_id, ns_email).first();
  if (existing) {
    await env.DB.prepare("DELETE FROM favorite_records WHERE id = ?").bind(existing.id).run();
    return json({ ok: true, favorited: false });
  } else {
    await env.DB.prepare(
      "INSERT INTO favorite_records (record_id, student_id, student_name, category_name, record_date, ns_path, created_by, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
    ).bind(record_id, student_id || "", student_name || "", category_name || "", record_date || "", ns_path || "", ns_email, Math.floor(Date.now() / 1e3)).run();
    return json({ ok: true, favorited: true });
  }
}
__name(handleFavoriteToggle, "handleFavoriteToggle");
__name2(handleFavoriteToggle, "handleFavoriteToggle");
async function handleFavoritesList(request, env, url) {
  const nsToken = url.searchParams.get("ns_token");
  const nsEmail = url.searchParams.get("ns_email");
  const nsServer = url.searchParams.get("ns_server");
  const valid = await validateNsToken(nsToken, nsEmail, nsServer);
  if (!valid) return json({ error: "Nieprawid\u0142owy lub wygas\u0142y token NS \u2014 zaloguj si\u0119 ponownie" }, 401);
  await ensureFavNoteColumn(env);
  const { results } = await env.DB.prepare(
    "SELECT record_id, student_id, student_name, category_name, record_date, ns_path, created_at, trainer_note, note_updated_at FROM favorite_records WHERE created_by = ? ORDER BY created_at DESC"
  ).bind(nsEmail).all();
  return json({ ok: true, favorites: results });
}
__name(handleFavoritesList, "handleFavoritesList");
__name2(handleFavoritesList, "handleFavoritesList");
var FAV_NOTE_READY = false;
async function ensureFavNoteColumn(env) {
  if (FAV_NOTE_READY) return;
  try {
    await env.DB.prepare("SELECT trainer_note, note_updated_at FROM favorite_records LIMIT 1").first();
  } catch (e) {
    try {
      await env.DB.prepare("ALTER TABLE favorite_records ADD COLUMN trainer_note TEXT").run();
    } catch (e2) {
    }
    try {
      await env.DB.prepare("ALTER TABLE favorite_records ADD COLUMN note_updated_at INTEGER").run();
    } catch (e3) {
    }
  }
  FAV_NOTE_READY = true;
}
__name(ensureFavNoteColumn, "ensureFavNoteColumn");
__name2(ensureFavNoteColumn, "ensureFavNoteColumn");
async function handleFavoriteNote(request, env) {
  let body = {};
  try {
    body = await request.json();
  } catch (e) {
  }
  const valid = await validateNsToken(body.ns_token, body.ns_email, body.ns_server);
  if (!valid) return json({ error: "Nieprawid\u0142owy lub wygas\u0142y token NS \u2014 zaloguj si\u0119 ponownie" }, 401);
  const rid = String(body.record_id || "").trim();
  if (!rid) return json({ error: "Brak record_id" }, 400);
  const note = String(body.note || "").trim().slice(0, 2e3);
  await ensureFavNoteColumn(env);
  const now = Math.floor(Date.now() / 1e3);
  const r = await env.DB.prepare("UPDATE favorite_records SET trainer_note = ?, note_updated_at = ? WHERE record_id = ? AND created_by = ?").bind(note || null, now, rid, body.ns_email).run();
  if (!r.meta || !r.meta.changes) return json({ error: "Tego nagrania nie ma w Twoich ulubionych" }, 404);
  return json({ ok: true, note: note || null, note_updated_at: now });
}
__name(handleFavoriteNote, "handleFavoriteNote");
__name2(handleFavoriteNote, "handleFavoriteNote");
async function handlePublicTemplatesList(env) {
  const { results } = await env.DB.prepare(
    "SELECT id, name, items, created_by, created_at FROM schedule_templates ORDER BY created_at DESC"
  ).all();
  const templates = results.map((t) => ({ ...t, items: JSON.parse(t.items) }));
  return json({ ok: true, templates });
}
__name(handlePublicTemplatesList, "handlePublicTemplatesList");
__name2(handlePublicTemplatesList, "handlePublicTemplatesList");
async function handlePublicTemplateCreate(request, env) {
  const body = await request.json();
  const { ns_token, ns_email, ns_server, name, items } = body;
  if (!name || !items || !items.length) return json({ error: "Brak nazwy lub pozycji szablonu" }, 400);
  const valid = await validateNsToken(ns_token, ns_email, ns_server);
  if (!valid) return json({ error: "Nieprawid\u0142owy lub wygas\u0142y token NS \u2014 zaloguj si\u0119 ponownie" }, 401);
  const result = await env.DB.prepare(
    "INSERT INTO schedule_templates (name, items, created_by, created_at) VALUES (?, ?, ?, ?)"
  ).bind(name, JSON.stringify(items), ns_email, Math.floor(Date.now() / 1e3)).run();
  return json({ ok: true, template_id: result.meta.last_row_id });
}
__name(handlePublicTemplateCreate, "handlePublicTemplateCreate");
__name2(handlePublicTemplateCreate, "handlePublicTemplateCreate");
async function handlePublicTemplateDelete(request, env, templateId) {
  const body = await request.json().catch(() => ({}));
  const { ns_token, ns_email, ns_server } = body;
  const valid = await validateNsToken(ns_token, ns_email, ns_server);
  if (!valid) return json({ error: "Nieprawid\u0142owy lub wygas\u0142y token NS \u2014 zaloguj si\u0119 ponownie" }, 401);
  await env.DB.prepare("DELETE FROM schedule_templates WHERE id = ?").bind(templateId).run();
  return json({ ok: true });
}
__name(handlePublicTemplateDelete, "handlePublicTemplateDelete");
__name2(handlePublicTemplateDelete, "handlePublicTemplateDelete");
async function handleRegister(request, env) {
  const body = await request.json();
  const {
    email,
    password,
    name,
    role,
    group_id,
    phone,
    city,
    preferred_lang,
    first_name,
    last_name,
    country,
    birthdate,
    therapy_beginning,
    diary_permission,
    records_permission,
    record_target_count,
    record_target_period
  } = body;
  if (!email || !password || !name) {
    return json({ error: "Brak wymaganych p\xF3l: email, has\u0142o, imi\u0119" }, 400);
  }
  const requestedRole = role || "student";
  const creator = await getUserFromToken(request, env);
  if (!creator || creator.role === "student") {
    return json({ error: "Tylko trener lub admin mo\u017Ce tworzy\u0107 nowe konta" }, 403);
  }
  if ((requestedRole === "trainer" || requestedRole === "admin") && creator.role !== "admin") {
    return json({ error: "Tylko admin mo\u017Ce tworzy\u0107 konta trener\xF3w/admin\xF3w" }, 403);
  }
  const existing = await env.DB.prepare("SELECT id FROM users WHERE email = ?").bind(email).first();
  if (existing) return json({ error: "Ten email jest ju\u017C zarejestrowany" }, 409);
  const baseSlug = slugify(name);
  let slug = baseSlug;
  let i = 2;
  while (await env.DB.prepare("SELECT id FROM users WHERE slug = ?").bind(slug).first()) {
    slug = baseSlug + "-" + i;
    i++;
  }
  const salt = generateToken().slice(0, 16);
  const passwordHash = await hashPassword(password, salt);
  const result = await env.DB.prepare(
    `INSERT INTO users (email, password_hash, role, name, first_name, last_name, slug, phone, city, country, birthdate, therapy_beginning, group_id, preferred_lang, diary_permission, records_permission, record_target_count, record_target_period)
     VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`
  ).bind(
    email,
    salt + ":" + passwordHash,
    role || "student",
    name,
    first_name || null,
    last_name || null,
    slug,
    phone || null,
    city || null,
    country || null,
    birthdate || null,
    therapy_beginning || null,
    group_id || null,
    preferred_lang || "pl",
    diary_permission ?? 1,
    records_permission ?? 1,
    record_target_count ?? 3,
    record_target_period || "week"
  ).run();
  return json({ ok: true, user_id: result.meta.last_row_id, slug });
}
__name(handleRegister, "handleRegister");
__name2(handleRegister, "handleRegister");
async function handleLogin(request, env) {
  const body = await request.json();
  const { email, password } = body;
  if (!email || !password) return json({ error: "Brak email/has\u0142a" }, 400);
  const user = await env.DB.prepare(
    "SELECT id, email, password_hash, role, name, slug, group_id, preferred_lang, active FROM users WHERE email = ?"
  ).bind(email).first();
  if (!user || !user.active) return json({ error: "Nieprawid\u0142owy email lub has\u0142o" }, 401);
  const [salt, storedHash] = user.password_hash.split(":");
  const computedHash = await hashPassword(password, salt);
  if (computedHash !== storedHash) return json({ error: "Nieprawid\u0142owy email lub has\u0142o" }, 401);
  const token = generateToken();
  const expiresAt = Math.floor(Date.now() / 1e3) + 30 * 24 * 60 * 60;
  await env.DB.prepare("INSERT INTO sessions (token, user_id, expires_at) VALUES (?, ?, ?)").bind(token, user.id, expiresAt).run();
  return json({
    ok: true,
    token,
    user: {
      id: user.id,
      email: user.email,
      role: user.role,
      name: user.name,
      slug: user.slug,
      group_id: user.group_id,
      preferred_lang: user.preferred_lang
    }
  });
}
__name(handleLogin, "handleLogin");
__name2(handleLogin, "handleLogin");
function getBearerToken(request) {
  const h = request.headers.get("Authorization") || "";
  const m = h.match(/^Bearer\s+(.+)$/i);
  if (m) return m[1];
  try {
    const url = new URL(request.url);
    const qToken = url.searchParams.get("token");
    if (qToken) return qToken;
  } catch (e) {
  }
  return null;
}
__name(getBearerToken, "getBearerToken");
__name2(getBearerToken, "getBearerToken");
async function getUserFromToken(request, env) {
  const token = getBearerToken(request);
  if (!token) return null;
  const now = Math.floor(Date.now() / 1e3);
  const session = await env.DB.prepare(
    "SELECT user_id, expires_at FROM sessions WHERE token = ?"
  ).bind(token).first();
  if (!session || session.expires_at < now) return null;
  const user = await env.DB.prepare(
    "SELECT id, email, role, name, slug, group_id, preferred_lang, active FROM users WHERE id = ?"
  ).bind(session.user_id).first();
  if (!user || !user.active) return null;
  return user;
}
__name(getUserFromToken, "getUserFromToken");
__name2(getUserFromToken, "getUserFromToken");
async function pollPostgresChanges(env) {
  const stateRow = await env.DB.prepare("SELECT value FROM sync_state WHERE key = ?").bind("last_postgres_poll_ts").first();
  const lastPollMs = stateRow ? parseInt(stateRow.value) : Date.now() - 5 * 60 * 1e3;
  const lastPollDate = new Date(lastPollMs);
  const nowMs = Date.now();
  const pgClient = new Client({ connectionString: env.HYPERDRIVE.connectionString });
  await pgClient.connect();
  let affectedNsIds = /* @__PURE__ */ new Set();
  let newStudentsRaw = [];
  try {
    const changed = await pgClient.query(
      `SELECT DISTINCT user_id AS ns_id FROM records WHERE updated_at > $1
       UNION
       SELECT DISTINCT user_id AS ns_id FROM diaries WHERE updated_at > $1
       UNION
       SELECT DISTINCT student_id AS ns_id FROM visit_dates WHERE updated_at > $1
       UNION
       SELECT DISTINCT vd.student_id AS ns_id
       FROM visit_record_requirements vrr JOIN visit_dates vd ON vd.id = vrr.visit_date_id
       WHERE vrr.updated_at > $1`,
      [lastPollDate]
    );
    changed.rows.forEach((r) => {
      if (r.ns_id) affectedNsIds.add(r.ns_id);
    });
    const newStudentsResult = await pgClient.query(
      `SELECT id, first_name, last_name, email, city, created_at
       FROM users WHERE role = 'student' AND deleted_at IS NULL AND created_at > $1`,
      [lastPollDate]
    );
    newStudentsRaw = newStudentsResult.rows;
  } finally {
    await pgClient.end();
  }
  let createdCount = 0, syncedCount = 0;
  const errors = [];
  for (const ns of newStudentsRaw) {
    try {
      const existing = await env.DB.prepare("SELECT id FROM users WHERE ns_legacy_id = ?").bind(ns.id).first();
      if (existing) continue;
      if (ns.email) {
        const emailMatch = await env.DB.prepare("SELECT id FROM users WHERE LOWER(email) = LOWER(?)").bind(ns.email).first();
        if (emailMatch) continue;
      }
      const createdTs = ns.created_at ? Math.floor(new Date(ns.created_at).getTime() / 1e3) : Math.floor(Date.now() / 1e3);
      const name = `${ns.first_name || ""} ${ns.last_name || ""}`.trim();
      const slug = name.toLowerCase().replace(/[^a-z0-9ąćęłńóśźż\s-]/g, "").replace(/\s+/g, "-");
      await env.DB.prepare(
        `INSERT INTO users (name, slug, email, city, role, active, group_id, ns_legacy_id, created_at, password_hash)
         VALUES (?, ?, ?, ?, 'student', 1, NULL, ?, ?, 'MIGRATED_NEEDS_RESET')`
      ).bind(name, slug, ns.email || null, ns.city || null, ns.id, createdTs).run();
      affectedNsIds.add(ns.id);
      createdCount++;
    } catch (e) {
      errors.push("create_student:" + ns.id + ":" + e.message);
    }
  }
  if (affectedNsIds.size) {
    const nsIdList = [...affectedNsIds];
    const placeholders = nsIdList.map(() => "?").join(",");
    const { results: localStudents } = await env.DB.prepare(
      `SELECT id, name, ns_legacy_id, created_at FROM users WHERE ns_legacy_id IN (${placeholders})`
    ).bind(...nsIdList).all();
    try {
      await speedPrecompute(env, nsIdList);
    } catch (e) {
      errors.push("precompute:" + e.message);
    }
    for (const student of localStudents) {
      try {
        await syncStudentRecordingsCore(env, student, null);
        await resyncStudentScheduleCore(env, student);
        syncedCount++;
      } catch (e) {
        errors.push("sync:" + student.id + ":" + e.message);
      }
    }
  }
  await env.DB.prepare(
    `INSERT INTO sync_state (key, value) VALUES ('last_postgres_poll_ts', ?)
     ON CONFLICT(key) DO UPDATE SET value = excluded.value`
  ).bind(String(nowMs)).run();
  return { created_students: createdCount, synced_students: syncedCount, affected_count: affectedNsIds.size, errors };
}
__name(pollPostgresChanges, "pollPostgresChanges");
__name2(pollPostgresChanges, "pollPostgresChanges");
async function speedPrecompute(env, nsIdList) {
  if (!nsIdList || !nsIdList.length) return;
  await speedTables(env);
  const pgClient = new Client({ connectionString: env.HYPERDRIVE.connectionString });
  let emails = [];
  await pgClient.connect();
  try {
    const r = await pgClient.query("SELECT email FROM users WHERE id::text = ANY($1::text[]) AND email IS NOT NULL", [nsIdList.map((x) => String(x))]);
    emails = r.rows.map((x) => String(x.email).toLowerCase());
  } finally {
    try {
      await pgClient.end();
    } catch (e) {
    }
  }
  for (const em of emails.slice(0, 40)) {
    try {
      await env.DB.prepare("DELETE FROM harmo_stats_cache WHERE email = ?").bind(em).run();
    } catch (e) {
    }
    try {
      const u = await env.DB.prepare("SELECT u.id, u.email FROM users u JOIN needs_correction_sync_state s ON s.user_id = u.id WHERE LOWER(u.email) = ?").bind(em).first();
      if (u) await syncNeedsCorrectionFromPostgres(env, u.id, u.email);
    } catch (e) {
    }
  }
}
__name(speedPrecompute, "speedPrecompute");
__name2(speedPrecompute, "speedPrecompute");
async function handleUploadRecording(request, env, user) {
  const formData = await request.formData();
  const file = formData.get("file");
  const categoryKey = formData.get("category_key");
  const lat = formData.get("lat") ? parseFloat(formData.get("lat")) : null;
  const lon = formData.get("lon") ? parseFloat(formData.get("lon")) : null;
  const city = formData.get("city") || null;
  const note = formData.get("note") || null;
  const emotion = formData.get("emotion") ? parseInt(formData.get("emotion")) : null;
  const nsSystem = formData.get("ns_system") || null;
  const durationSec = formData.get("duration_sec") ? parseFloat(formData.get("duration_sec")) : null;
  const rmsDb = formData.get("rms_db") ? parseFloat(formData.get("rms_db")) : null;
  const snrDb = formData.get("snr_db") ? parseFloat(formData.get("snr_db")) : null;
  const speechPct = formData.get("speech_pct") ? parseFloat(formData.get("speech_pct")) : null;
  if (!file || !categoryKey) {
    return json({ error: "Brak pliku lub kategorii" }, 400);
  }
  const category = await env.DB.prepare("SELECT id FROM categories WHERE key = ?").bind(categoryKey).first();
  if (!category) return json({ error: "Nieznana kategoria: " + categoryKey }, 400);
  let qualityFlag = null;
  if (durationSec !== null && durationSec < 2) qualityFlag = "too_short";
  else if (rmsDb !== null && rmsDb < -45) qualityFlag = "silent";
  else if (rmsDb !== null && rmsDb < -35) qualityFlag = "too_quiet";
  else if (speechPct !== null && speechPct < 15) qualityFlag = "low_speech_pct";
  const ts = Date.now();
  const ext = (file.name.match(/\.[a-z0-9]+$/i) || [".webm"])[0];
  const safeFilename = `${ts}${ext}`;
  const r2Key = `${user.slug}/${categoryKey}/${safeFilename}`;
  await env.RECORDINGS.put(r2Key, await file.arrayBuffer(), {
    httpMetadata: { contentType: file.type || "audio/webm" }
  });
  const existingCount = await env.DB.prepare(
    "SELECT COUNT(*) as cnt FROM recordings WHERE user_id = ? AND category_id = ?"
  ).bind(user.id, category.id).first();
  const isBaseline = existingCount.cnt === 0 ? 1 : 0;
  const status = qualityFlag ? "auto_flagged" : "pending";
  const result = await env.DB.prepare(
    `INSERT INTO recordings
     (user_id, category_id, r2_key, filename, duration_sec, file_size_bytes, lat, lon, city, note, emotion, ns_system, status, quality_flag, rms_db, snr_db, speech_pct, is_baseline)
     VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`
  ).bind(
    user.id,
    category.id,
    r2Key,
    file.name || safeFilename,
    durationSec,
    file.size || null,
    lat,
    lon,
    city,
    note,
    emotion,
    nsSystem,
    status,
    qualityFlag,
    rmsDb,
    snrDb,
    speechPct,
    isBaseline
  ).run();
  return json({
    ok: true,
    recording_id: result.meta.last_row_id,
    r2_key: r2Key,
    quality_flag: qualityFlag,
    status,
    message: qualityFlag ? qualityFlagMessage(qualityFlag) : null
  });
}
__name(handleUploadRecording, "handleUploadRecording");
__name2(handleUploadRecording, "handleUploadRecording");
function qualityFlagMessage(flag) {
  const messages = {
    too_short: "Nagranie jest bardzo kr\xF3tkie \u2014 spr\xF3buj nagra\u0107 d\u0142u\u017Csz\u0105 wypowied\u017A.",
    silent: "Nagranie wygl\u0105da na puste/ciche \u2014 sprawd\u017A mikrofon i nagraj ponownie.",
    too_quiet: "Nagranie jest bardzo ciche \u2014 m\xF3w g\u0142o\u015Bniej albo sprawd\u017A mikrofon.",
    low_speech_pct: "W nagraniu wykryto du\u017Co ciszy \u2014 spr\xF3buj nagra\u0107 ponownie."
  };
  return messages[flag] || null;
}
__name(qualityFlagMessage, "qualityFlagMessage");
__name2(qualityFlagMessage, "qualityFlagMessage");
async function handleListRecordings(request, env, user) {
  const url = new URL(request.url);
  const pageSize = Math.min(parseInt(url.searchParams.get("page_size") || "50"), 500);
  let query, params;
  if (user.role === "admin") {
    query = `SELECT r.*, c.key as category_key, c.name_pl as category_name, u.name as student_name
              FROM recordings r
              JOIN categories c ON r.category_id = c.id
              JOIN users u ON r.user_id = u.id
              ORDER BY r.created_at DESC LIMIT ?`;
    params = [pageSize];
  } else if (user.role === "trainer") {
    query = `SELECT r.*, c.key as category_key, c.name_pl as category_name, u.name as student_name
              FROM recordings r
              JOIN categories c ON r.category_id = c.id
              JOIN users u ON r.user_id = u.id
              WHERE u.group_id IN (SELECT group_id FROM trainer_group_access WHERE trainer_id = ?)
              ORDER BY r.created_at DESC LIMIT ?`;
    params = [user.id, pageSize];
  } else {
    query = `SELECT r.*, c.key as category_key, c.name_pl as category_name
              FROM recordings r
              JOIN categories c ON r.category_id = c.id
              WHERE r.user_id = ?
              ORDER BY r.created_at DESC LIMIT ?`;
    params = [user.id, pageSize];
  }
  const { results } = await env.DB.prepare(query).bind(...params).all();
  return json({ ok: true, recordings: results });
}
__name(handleListRecordings, "handleListRecordings");
__name2(handleListRecordings, "handleListRecordings");
async function handleListGroupsDetailed(env) {
  const { results: groups } = await env.DB.prepare("SELECT id, name_pl FROM groups ORDER BY name_pl").all();
  for (const g of groups) {
    const { results: trainers } = await env.DB.prepare(
      `SELECT u.id, u.name FROM trainer_group_access tga JOIN users u ON tga.trainer_id = u.id WHERE tga.group_id = ?`
    ).bind(g.id).all();
    g.trainers = trainers;
    const studentCount = await env.DB.prepare(
      `SELECT COUNT(*) as cnt FROM users WHERE group_id = ? AND role = 'student' AND active = 1`
    ).bind(g.id).first();
    g.student_count = studentCount.cnt;
  }
  return json({ ok: true, groups });
}
__name(handleListGroupsDetailed, "handleListGroupsDetailed");
__name2(handleListGroupsDetailed, "handleListGroupsDetailed");
async function handleHarmonogramOverview(env, trainer) {
  const isAdmin = trainer.role === "admin";
  const groupFilter = isAdmin ? "1=1" : "u.group_id IN (SELECT group_id FROM trainer_group_access WHERE trainer_id = ?)";
  const nowTs = Math.floor(Date.now() / 1e3);
  const bindArg = isAdmin ? [nowTs, nowTs] : [trainer.id, nowTs, nowTs];
  const { results } = await env.DB.prepare(
    `SELECT u.id as student_id, u.name as student_name, g.name_pl as group_name,
       SUM(r.required_count) as total_required,
       SUM(MIN(r.required_count, COALESCE(rc.cnt, 0))) as total_correct_capped,
       COUNT(*) as req_count
     FROM requirements r
     JOIN users u ON r.user_id = u.id
     LEFT JOIN groups g ON u.group_id = g.id
     LEFT JOIN (
       SELECT rec.user_id as uid, rec.category_id as cid, rq.period_start as pstart, rq.period_end as pend, COUNT(*) as cnt
       FROM recordings rec
       JOIN requirements rq ON rq.user_id = rec.user_id AND rq.category_id = rec.category_id
         AND rec.created_at >= rq.period_start AND rec.created_at <= rq.period_end
       WHERE rec.status='correct'
       GROUP BY rec.user_id, rec.category_id, rq.period_start, rq.period_end
     ) rc ON rc.uid = r.user_id AND rc.cid = r.category_id
       AND rc.pstart = r.period_start AND rc.pend = r.period_end
     WHERE ${groupFilter} AND r.period_start <= ? AND r.period_end >= ?
     GROUP BY u.id, u.name, g.name_pl
     ORDER BY u.name`
  ).bind(...bindArg).all();
  const rows = results.map((row) => ({
    student_id: row.student_id,
    student_name: row.student_name,
    group_name: row.group_name,
    pct: row.total_required > 0 ? Math.round(row.total_correct_capped / row.total_required * 100) : null
  }));
  return json({ ok: true, rows });
}
__name(handleHarmonogramOverview, "handleHarmonogramOverview");
__name2(handleHarmonogramOverview, "handleHarmonogramOverview");
async function handleListStudents(request, env, user) {
  const url = new URL(request.url);
  const search = url.searchParams.get("search");
  const searchClause = search ? `AND (u.name LIKE ? OR u.email LIKE ?)` : "";
  const searchParams = search ? [`%${search}%`, `%${search}%`] : [];
  let query, params;
  if (user.role === "admin") {
    query = `SELECT u.id, u.name, u.slug, u.email, u.city, g.name_pl as group_name
             FROM users u LEFT JOIN groups g ON u.group_id = g.id
             WHERE u.role = 'student' AND u.active = 1 ${searchClause} ORDER BY u.created_at DESC`;
    params = [...searchParams];
  } else {
    query = `SELECT u.id, u.name, u.slug, u.email, u.city, g.name_pl as group_name
             FROM users u LEFT JOIN groups g ON u.group_id = g.id
             WHERE u.role = 'student' AND u.active = 1
             AND u.group_id IN (SELECT group_id FROM trainer_group_access WHERE trainer_id = ?)
             ${searchClause} ORDER BY u.created_at DESC`;
    params = [user.id, ...searchParams];
  }
  const { results } = await env.DB.prepare(query).bind(...params).all();
  return json({ ok: true, students: results });
}
__name(handleListStudents, "handleListStudents");
__name2(handleListStudents, "handleListStudents");
async function handleUpdateStudent(request, env, trainer, studentId) {
  const body = await request.json();
  const student = await env.DB.prepare("SELECT group_id FROM users WHERE id = ?").bind(studentId).first();
  if (!student) return json({ error: "Nie znaleziono kursanta" }, 404);
  if (trainer.role !== "admin") {
    const access = await env.DB.prepare(
      "SELECT 1 FROM trainer_group_access WHERE trainer_id = ? AND group_id = ?"
    ).bind(trainer.id, student.group_id).first();
    if (!access) return json({ error: "Brak dost\u0119pu" }, 403);
  }
  const editable = [
    "first_name",
    "last_name",
    "name",
    "phone",
    "city",
    "country",
    "birthdate",
    "facebook_profile",
    "therapy_beginning",
    "therapy_end",
    "my_history",
    "my_history_public",
    "my_nowamowa",
    "my_nowamowa_public",
    "trainer_notes",
    "diary_permission",
    "records_permission",
    "max_records_count",
    "max_inactivity_unit",
    "max_inactivity_value",
    "record_target_period",
    "record_target_count",
    "diary_supervision_end",
    "record_supervision_end",
    "diary_template",
    "group_id"
  ];
  const setClauses = [];
  const values = [];
  for (const field of editable) {
    if (body.hasOwnProperty(field)) {
      setClauses.push(`${field} = ?`);
      values.push(body[field]);
    }
  }
  if (!setClauses.length) return json({ error: "Brak p\xF3l do zaktualizowania" }, 400);
  setClauses.push("updated_at = ?");
  values.push(Math.floor(Date.now() / 1e3));
  values.push(studentId);
  await env.DB.prepare(`UPDATE users SET ${setClauses.join(", ")} WHERE id = ?`).bind(...values).run();
  return json({ ok: true });
}
__name(handleUpdateStudent, "handleUpdateStudent");
__name2(handleUpdateStudent, "handleUpdateStudent");
async function handleReviewRecording(request, env, trainer, recordingId) {
  const body = await request.json();
  const { status, star_rating, trainer_comment } = body;
  const validStatuses = ["correct", "needs_improvement", "pending"];
  if (status && !validStatuses.includes(status)) {
    return json({ error: "Nieprawid\u0142owy status" }, 400);
  }
  if (star_rating !== void 0 && star_rating !== null && (star_rating < 1 || star_rating > 5)) {
    return json({ error: "Gwiazdki musz\u0105 by\u0107 1-5" }, 400);
  }
  const rec = await env.DB.prepare(
    `SELECT r.id, r.user_id, u.group_id FROM recordings r JOIN users u ON r.user_id = u.id WHERE r.id = ?`
  ).bind(recordingId).first();
  if (!rec) return json({ error: "Nie znaleziono nagrania" }, 404);
  if (trainer.role !== "admin") {
    const access = await env.DB.prepare(
      "SELECT 1 FROM trainer_group_access WHERE trainer_id = ? AND group_id = ?"
    ).bind(trainer.id, rec.group_id).first();
    if (!access) return json({ error: "Brak dost\u0119pu do tego kursanta" }, 403);
  }
  const now = Math.floor(Date.now() / 1e3);
  await env.DB.prepare(
    `UPDATE recordings SET status = COALESCE(?, status), star_rating = COALESCE(?, star_rating),
     trainer_comment = COALESCE(?, trainer_comment), reviewed_at = ?, reviewed_by = ? WHERE id = ?`
  ).bind(status || null, star_rating ?? null, trainer_comment || null, now, trainer.id, recordingId).run();
  await env.DB.prepare(
    `INSERT INTO notifications (user_id, type, params, related_id) VALUES (?, 'recording_reviewed', ?, ?)`
  ).bind(rec.user_id, JSON.stringify({ status, star_rating }), recordingId).run();
  return json({ ok: true });
}
__name(handleReviewRecording, "handleReviewRecording");
__name2(handleReviewRecording, "handleReviewRecording");
async function handleUploadCorrection(request, env, trainer, recordingId) {
  const rec = await env.DB.prepare(
    `SELECT r.id, r.user_id, r.category_id, u.slug, u.group_id, c.key as category_key
     FROM recordings r JOIN users u ON r.user_id = u.id JOIN categories c ON r.category_id = c.id
     WHERE r.id = ?`
  ).bind(recordingId).first();
  if (!rec) return json({ error: "Nie znaleziono nagrania" }, 404);
  if (trainer.role !== "admin") {
    const access = await env.DB.prepare(
      "SELECT 1 FROM trainer_group_access WHERE trainer_id = ? AND group_id = ?"
    ).bind(trainer.id, rec.group_id).first();
    if (!access) return json({ error: "Brak dost\u0119pu do tego kursanta" }, 403);
  }
  const formData = await request.formData();
  const file = formData.get("file");
  const note = formData.get("note") || null;
  if (!file) return json({ error: "Brak pliku" }, 400);
  const ts = Date.now();
  const ext = (file.name.match(/\.[a-z0-9]+$/i) || [".webm"])[0];
  const r2Key = `${rec.slug}/${rec.category_key}/corrections/${ts}${ext}`;
  await env.RECORDINGS.put(r2Key, await file.arrayBuffer(), {
    httpMetadata: { contentType: file.type || "audio/webm" }
  });
  const result = await env.DB.prepare(
    `INSERT INTO recording_corrections (recording_id, trainer_id, r2_key, note) VALUES (?, ?, ?, ?)`
  ).bind(recordingId, trainer.id, r2Key, note).run();
  await env.DB.prepare(
    `INSERT INTO notifications (user_id, type, params, related_id) VALUES (?, 'correction_added', '{}', ?)`
  ).bind(rec.user_id, recordingId).run();
  return json({ ok: true, correction_id: result.meta.last_row_id, r2_key: r2Key });
}
__name(handleUploadCorrection, "handleUploadCorrection");
__name2(handleUploadCorrection, "handleUploadCorrection");
async function handleSetRequirement(request, env, trainer) {
  const body = await request.json();
  const { user_id, category_key, required_count, period_start, period_end } = body;
  if (!user_id || !category_key || !required_count) {
    return json({ error: "Brak wymaganych p\xF3l: user_id, category_key, required_count" }, 400);
  }
  const category = await env.DB.prepare("SELECT id FROM categories WHERE key = ?").bind(category_key).first();
  if (!category) return json({ error: "Nieznana kategoria" }, 400);
  const student = await env.DB.prepare("SELECT group_id FROM users WHERE id = ?").bind(user_id).first();
  if (!student) return json({ error: "Nie znaleziono kursanta" }, 404);
  if (trainer.role !== "admin") {
    const access = await env.DB.prepare(
      "SELECT 1 FROM trainer_group_access WHERE trainer_id = ? AND group_id = ?"
    ).bind(trainer.id, student.group_id).first();
    if (!access) return json({ error: "Brak dost\u0119pu do tego kursanta" }, 403);
  }
  const result = await env.DB.prepare(
    `INSERT INTO requirements (user_id, category_id, required_count, period_start, period_end) VALUES (?, ?, ?, ?, ?)`
  ).bind(user_id, category.id, required_count, period_start || null, period_end || null).run();
  return json({ ok: true, requirement_id: result.meta.last_row_id });
}
__name(handleSetRequirement, "handleSetRequirement");
__name2(handleSetRequirement, "handleSetRequirement");
async function handleGetRequirements(request, env, user) {
  const url = new URL(request.url);
  const targetUserId = url.searchParams.get("user_id") ? parseInt(url.searchParams.get("user_id")) : user.id;
  if (user.role === "student" && targetUserId !== user.id) {
    return json({ error: "Brak uprawnie\u0144" }, 403);
  }
  const { results: reqs } = await env.DB.prepare(
    `SELECT r.id, r.category_id, c.key as category_key, c.name_pl, c.name_en, c.name_cz,
            r.required_count, r.period_start, r.period_end
     FROM requirements r JOIN categories c ON r.category_id = c.id
     WHERE r.user_id = ?`
  ).bind(targetUserId).all();
  const progress = [];
  for (const req of reqs) {
    let count;
    if (req.period_start && req.period_end) {
      count = await env.DB.prepare(
        `SELECT COUNT(*) as sent,
                SUM(CASE WHEN status='correct' THEN 1 ELSE 0 END) as correct,
                SUM(CASE WHEN status='needs_improvement' THEN 1 ELSE 0 END) as needs_correction
         FROM recordings WHERE user_id = ? AND category_id = ? AND created_at >= ? AND created_at <= ?`
      ).bind(targetUserId, req.category_id, req.period_start, req.period_end).first();
    } else {
      count = await env.DB.prepare(
        `SELECT COUNT(*) as sent,
                SUM(CASE WHEN status='correct' THEN 1 ELSE 0 END) as correct,
                SUM(CASE WHEN status='needs_improvement' THEN 1 ELSE 0 END) as needs_correction
         FROM recordings WHERE user_id = ? AND category_id = ?`
      ).bind(targetUserId, req.category_id).first();
    }
    progress.push({ ...req, correct_count: count.correct || 0, sent_count: count.sent || 0, needs_correction_count: count.needs_correction || 0 });
  }
  return json({ ok: true, requirements: progress });
}
__name(handleGetRequirements, "handleGetRequirements");
__name2(handleGetRequirements, "handleGetRequirements");
async function handleFetchRecordingAudio(request, env) {
  const body = await request.json();
  const { recording_id, ns_token } = body;
  if (!recording_id || !ns_token) return json({ error: "Brak recording_id lub ns_token" }, 400);
  const rec = await env.DB.prepare("SELECT id, ns_legacy_id, r2_key FROM recordings WHERE id = ?").bind(recording_id).first();
  if (!rec) return json({ error: "Nie znaleziono nagrania" }, 404);
  if (!rec.ns_legacy_id) return json({ error: "To nagranie nie ma powi\u0105zania z NS (dodane r\u0119cznie?)" }, 400);
  if (rec.r2_key && !rec.r2_key.startsWith("_ns_import") && !rec.r2_key.startsWith("_no_audio_imported")) {
    return json({ ok: true, already_fetched: true });
  }
  const NS_HEADERS = {
    "Accept": "application/json",
    "Authorization": `Token token=${ns_token}`,
    "Email": "nowamowa@gmail.com",
    "Referer": "https://ns.nowamowa.com/front/index.html",
    "X-Requested-With": "XMLHttpRequest"
  };
  const nsResp = await fetch(`https://ns.nowamowa.com/api/v1/records/${rec.ns_legacy_id}`, { headers: NS_HEADERS });
  if (!nsResp.ok) return json({ error: "Nie uda\u0142o si\u0119 pobra\u0107 danych z NS (status " + nsResp.status + ") \u2014 mo\u017Ce token wygas\u0142?" }, 502);
  const nsData = await nsResp.json();
  let fileUrl = nsData.record_file_url;
  if (!fileUrl) return json({ error: "To nagranie nie ma pliku audio w NS" }, 404);
  if (fileUrl.startsWith("/public/")) fileUrl = fileUrl.slice("/public".length);
  const fullUrl = "https://ns.nowamowa.com" + fileUrl;
  const audioResp = await fetch(fullUrl, { headers: NS_HEADERS });
  if (!audioResp.ok) return json({ error: "Nie uda\u0142o si\u0119 pobra\u0107 samego pliku audio (status " + audioResp.status + ")" }, 502);
  const audioBuffer = await audioResp.arrayBuffer();
  const newR2Key = `recordings/${recording_id}.mp3`;
  await env.RECORDINGS.put(newR2Key, audioBuffer, { httpMetadata: { contentType: "audio/mpeg" } });
  await env.DB.prepare("UPDATE recordings SET r2_key = ? WHERE id = ?").bind(newR2Key, recording_id).run();
  return json({ ok: true, already_fetched: false, size_bytes: audioBuffer.byteLength });
}
__name(handleFetchRecordingAudio, "handleFetchRecordingAudio");
__name2(handleFetchRecordingAudio, "handleFetchRecordingAudio");
async function handleNsAutoLogin(request, env) {
  const body = await request.json();
  const { email, password } = body;
  if (!email || !password) return json({ error: "Brak email lub has\u0142a" }, 400);
  const candidates = [
    "https://ns.nowamowa.com/api/v1/session",
    "https://ns.nowamowa.com/api/v1/users/sign_in",
    "https://ns.nowamowa.com/api/v1/sign_in"
  ];
  for (const url of candidates) {
    try {
      const r = await fetch(url, {
        method: "POST",
        headers: { "Content-Type": "application/json", "Accept": "application/json" },
        body: JSON.stringify({ email, password })
      });
      if (!r.ok) continue;
      const d = await r.json();
      const token = d.auth_token || d.token || d.jwt || d.data && d.data.auth_token;
      if (token) return json({ ok: true, ns_token: token, endpoint_used: url });
    } catch (e) {
    }
  }
  return json({ error: "Nie uda\u0142o si\u0119 zalogowa\u0107 do NS \u017Cadnym ze znanych adres\xF3w \u2014 mo\u017Ce dane logowania si\u0119 r\xF3\u017Cni\u0105, albo trzeba dalej podawa\u0107 token r\u0119cznie" }, 502);
}
__name(handleNsAutoLogin, "handleNsAutoLogin");
__name2(handleNsAutoLogin, "handleNsAutoLogin");
async function handleNsListStudents(request, env) {
  const pgClient = new Client({ connectionString: env.HYPERDRIVE.connectionString });
  let all = [];
  try {
    await pgClient.connect();
    const result = await pgClient.query(
      `SELECT id, first_name, last_name, email
       FROM users
       WHERE role = 'student' AND deleted_at IS NULL
       ORDER BY created_at DESC`
    );
    all = result.rows;
  } finally {
    await pgClient.end();
  }
  const { results: existing } = await env.DB.prepare("SELECT ns_legacy_id FROM users WHERE ns_legacy_id IS NOT NULL").all();
  const existingIds = new Set(existing.map((r) => r.ns_legacy_id));
  const students = all.map((s) => ({
    ns_id: s.id,
    first_name: s.first_name,
    last_name: s.last_name,
    email: s.email,
    exists_locally: existingIds.has(s.id)
  }));
  return json({ ok: true, students, total: students.length, new_count: students.filter((s) => !s.exists_locally).length, source: "postgres_hyperdrive" });
}
__name(handleNsListStudents, "handleNsListStudents");
__name2(handleNsListStudents, "handleNsListStudents");
async function handleCreateStudentFromNs(request, env) {
  const body = await request.json();
  const { ns_id } = body;
  if (!ns_id) return json({ error: "Brak ns_id" }, 400);
  const existing = await env.DB.prepare("SELECT id FROM users WHERE ns_legacy_id = ?").bind(ns_id).first();
  if (existing) return json({ ok: true, user_id: existing.id, already_existed: true });
  const pgClient = new Client({ connectionString: env.HYPERDRIVE.connectionString });
  let d;
  try {
    await pgClient.connect();
    const result2 = await pgClient.query(
      `SELECT id, first_name, last_name, email, city, created_at FROM users WHERE id = $1`,
      [ns_id]
    );
    if (!result2.rows.length) return json({ error: "Nie znaleziono kursanta w NS" }, 404);
    d = result2.rows[0];
  } finally {
    await pgClient.end();
  }
  if (d.email) {
    const emailMatch = await env.DB.prepare("SELECT id, name FROM users WHERE LOWER(email) = LOWER(?)").bind(d.email).first();
    if (emailMatch) {
      return json({ ok: true, user_id: emailMatch.id, already_existed: true, skipped_email_duplicate: true, note: "Pomini\u0119to \u2014 istnieje ju\u017C konto z tym samym mailem (" + emailMatch.name + "), prawdopodobnie duplikat po stronie NS." });
    }
  }
  const createdTs = d.created_at ? Math.floor(new Date(d.created_at).getTime() / 1e3) : Math.floor(Date.now() / 1e3);
  const name = `${d.first_name || ""} ${d.last_name || ""}`.trim();
  const slug = name.toLowerCase().replace(/[^a-z0-9ąćęłńóśźż\s-]/g, "").replace(/\s+/g, "-");
  const result = await env.DB.prepare(
    `INSERT INTO users (name, slug, email, city, role, active, group_id, ns_legacy_id, created_at, password_hash)
     VALUES (?, ?, ?, ?, 'student', 1, NULL, ?, ?, 'MIGRATED_NEEDS_RESET')`
  ).bind(name, slug, d.email || null, d.city || null, d.id, createdTs).run();
  return json({ ok: true, user_id: result.meta.last_row_id, already_existed: false, source: "postgres_hyperdrive" });
}
__name(handleCreateStudentFromNs, "handleCreateStudentFromNs");
__name2(handleCreateStudentFromNs, "handleCreateStudentFromNs");
async function handleGetGpsPoints(request, env) {
  const body = await request.json();
  const { ns_token } = body;
  if (!ns_token) return json({ error: "Brak ns_token" }, 400);
  const r = await fetch("https://pitchrec-proxy.mowaiemocje.workers.dev/map/points", {
    headers: { "Authorization": "Token token=" + ns_token, "Email": "nowamowa@gmail.com" }
  });
  if (!r.ok) return json({ error: "Nie uda\u0142o si\u0119 pobra\u0107 punkt\xF3w z mapy (status " + r.status + ")" }, 502);
  const d = await r.json();
  return json({ ok: true, history: d.history || [] });
}
__name(handleGetGpsPoints, "handleGetGpsPoints");
__name2(handleGetGpsPoints, "handleGetGpsPoints");
async function handleTrainerGpsPoints(request, env, url) {
  const nsToken = url.searchParams.get("ns_token");
  const nsEmail = url.searchParams.get("ns_email");
  const nsServer = url.searchParams.get("ns_server");
  const valid = await validateNsToken(nsToken, nsEmail, nsServer);
  if (!valid) return json({ error: "Nieprawid\u0142owy lub wygas\u0142y token NS" }, 401);
  try {
    const r = await fetch("https://pitchrec-proxy.mowaiemocje.workers.dev/map/points", {
      headers: {
        "Authorization": "Token token=" + nsToken,
        "Email": nsEmail,
        "Accept": "application/json",
        "User-Agent": "Mozilla/5.0 (compatible; NewspeechBackend/1.0)"
      }
    });
    if (!r.ok) {
      const bodyText = await r.text().catch(() => "");
      return json({ error: "Nie uda\u0142o si\u0119 pobra\u0107 punkt\xF3w z mapy (status " + r.status + "): " + bodyText.slice(0, 300) }, 502);
    }
    const d = await r.json();
    return json({ ok: true, history: d.history || [] });
  } catch (e) {
    return json({ error: "B\u0142\u0105d: " + e.message }, 500);
  }
}
__name(handleTrainerGpsPoints, "handleTrainerGpsPoints");
__name2(handleTrainerGpsPoints, "handleTrainerGpsPoints");
async function syncStudentRecordingsCore(env, student, ns_token) {
  const latest = await env.DB.prepare("SELECT MAX(created_at) as ts FROM recordings WHERE user_id = ?").bind(student.id).first();
  const sinceMs = (latest.ts || 0) * 1e3;
  const sinceDate = new Date(sinceMs);
  let gpsPoints = [];
  if (ns_token) {
    try {
      const gpsResp = await fetch("https://pitchrec-proxy.mowaiemocje.workers.dev/map/points", {
        headers: { "Authorization": "Token token=" + ns_token, "Email": "nowamowa@gmail.com" }
      });
      if (gpsResp.ok) {
        const gd = await gpsResp.json();
        gpsPoints = gd.history || [];
      }
    } catch (e) {
    }
  }
  function matchGps(categoryNamePl, recordingCreatedTs) {
    const nameLower = student.name.toLowerCase().trim();
    let best = null, bestDiff = Infinity;
    for (const p of gpsPoints) {
      if (!p.name || !p.cat || !p.lat || !p.lon) continue;
      if (p.name.toLowerCase().trim() !== nameLower) continue;
      if (p.cat.toLowerCase().trim() !== categoryNamePl.toLowerCase().trim()) continue;
      const diff = Math.abs((p.ts || 0) - recordingCreatedTs * 1e3);
      if (diff < bestDiff && diff < 12 * 60 * 60 * 1e3) {
        bestDiff = diff;
        best = p;
      }
    }
    return best;
  }
  __name(matchGps, "matchGps");
  __name2(matchGps, "matchGps");
  let newRecordings = 0, newDiary = 0;
  let debugInfo = { total_fetched: 0, after_filter: 0, category_misses: [], fetch_error: null, source: "postgres_hyperdrive" };
  const pgClient = new Client({ connectionString: env.HYPERDRIVE.connectionString });
  await pgClient.connect();
  try {
    const recResult = await pgClient.query(
      `SELECT r.id, r.date, r.created_at, r.is_correct, r.description,
              COALESCE(rct.name, rc.name) AS category_name_pl
       FROM records r
       JOIN record_categories rc ON rc.id = r.record_category_id
       LEFT JOIN record_category_translations rct ON rct.record_category_id = rc.id AND rct.locale = 'pl'
       WHERE r.user_id = $1 AND r.created_at > $2 AND r.deleted_at IS NULL
       ORDER BY r.created_at DESC
       LIMIT 200`,
      [student.ns_legacy_id, sinceDate]
    );
    debugInfo.total_fetched = recResult.rows.length;
    debugInfo.after_filter = recResult.rows.length;
    const categoryCache = {};
    const stmts = [];
    for (const r of recResult.rows) {
      if (!r.category_name_pl) {
        debugInfo.category_misses.push("(brak nazwy kategorii)");
        continue;
      }
      if (!(r.category_name_pl in categoryCache)) {
        const cat = await env.DB.prepare("SELECT id FROM categories WHERE name_pl = ?").bind(r.category_name_pl).first();
        categoryCache[r.category_name_pl] = cat ? cat.id : null;
      }
      const catId = categoryCache[r.category_name_pl];
      if (!catId) {
        debugInfo.category_misses.push(r.category_name_pl);
        continue;
      }
      const r2Key = `_ns_import/${r.id}`;
      const createdTs = Math.floor(new Date(r.created_at).getTime() / 1e3);
      const mappedStatus = r.is_correct === true ? "correct" : "pending";
      const gpsMatch = matchGps(r.category_name_pl, createdTs);
      stmts.push(env.DB.prepare(
        `INSERT INTO recordings (user_id, category_id, r2_key, filename, note, status, created_at, ns_legacy_id, lat, lon)
         VALUES (?, ?, ?, '(import z bazy NS)', ?, ?, ?, ?, ?, ?)
         ON CONFLICT(r2_key) DO NOTHING`
      ).bind(
        student.id,
        catId,
        r2Key,
        r.description || null,
        mappedStatus,
        createdTs,
        r.id,
        gpsMatch ? gpsMatch.lat : null,
        gpsMatch ? gpsMatch.lon : null
      ));
    }
    if (stmts.length) {
      const BATCH = 30;
      for (let i = 0; i < stmts.length; i += BATCH) {
        const result = await env.DB.batch(stmts.slice(i, i + BATCH));
        newRecordings += result.filter((r) => r.meta.changes > 0).length;
      }
    }
    const diaryResult = await pgClient.query(
      `SELECT id, date, notes, is_correct, meditation_count
       FROM diaries
       WHERE user_id = $1 AND deleted_at IS NULL
       ORDER BY date DESC
       LIMIT 30`,
      [student.ns_legacy_id]
    );
    for (const entry of diaryResult.rows) {
      const entryDate = entry.date instanceof Date ? entry.date.toISOString().slice(0, 10) : String(entry.date).slice(0, 10);
      if (!entryDate) continue;
      const result = await env.DB.prepare(
        `INSERT INTO diary_entries (user_id, entry_date, notes, custom_fields, is_correct)
         SELECT ?, ?, ?, ?, ?
         WHERE NOT EXISTS (SELECT 1 FROM diary_entries WHERE user_id = ? AND entry_date = ?)`
      ).bind(
        student.id,
        entryDate,
        dedupeRepeatedText(entry.notes) || null,
        JSON.stringify(entry),
        entry.is_correct != null ? String(entry.is_correct) : null,
        student.id,
        entryDate
      ).run();
      if (result.meta.changes > 0) newDiary++;
    }
  } catch (e) {
    debugInfo.fetch_error = e.message;
  } finally {
    await pgClient.end();
  }
  return { new_recordings: newRecordings, new_diary: newDiary, debug: debugInfo, since_ts: latest.ts };
}
__name(syncStudentRecordingsCore, "syncStudentRecordingsCore");
__name2(syncStudentRecordingsCore, "syncStudentRecordingsCore");
async function handleSyncStudentRecordings(request, env) {
  const body = await request.json();
  const { user_id, ns_token } = body;
  const student = await env.DB.prepare("SELECT id, name, ns_legacy_id FROM users WHERE id = ?").bind(user_id).first();
  if (!student || !student.ns_legacy_id) return json({ ok: true, new_recordings: 0, new_diary: 0 });
  const result = await syncStudentRecordingsCore(env, student, ns_token);
  return json({ ok: true, ...result });
}
__name(handleSyncStudentRecordings, "handleSyncStudentRecordings");
__name2(handleSyncStudentRecordings, "handleSyncStudentRecordings");
async function handleReviewQueue(env, user) {
  const isAdmin = user.role === "admin";
  const groupFilter = isAdmin ? "1=1" : "u.group_id IN (SELECT group_id FROM trainer_group_access WHERE trainer_id = ?)";
  const bindArg = isAdmin ? [] : [user.id];
  const { results } = await env.DB.prepare(
    `SELECT r.id, r.ns_legacy_id, r.r2_key, r.note, r.created_at,
            u.id as student_id, u.name as student_name,
            c.name_pl as category_name
     FROM recordings r JOIN users u ON r.user_id = u.id JOIN categories c ON r.category_id = c.id
     WHERE r.status = 'pending' AND ${groupFilter}
     ORDER BY r.created_at ASC LIMIT 30`
  ).bind(...bindArg).all();
  return json({ ok: true, queue: results });
}
__name(handleReviewQueue, "handleReviewQueue");
__name2(handleReviewQueue, "handleReviewQueue");
async function resyncStudentScheduleCore(env, student) {
  const timing = {};
  const t0 = Date.now();
  const pgClient = new Client({ connectionString: env.HYPERDRIVE.connectionString });
  await pgClient.connect();
  timing.pg_connect_ms = Date.now() - t0;
  let visits = [];
  try {
    const tQueryStart = Date.now();
    const visitResult = await pgClient.query(
      `SELECT vd.id AS visit_id, vd.date AS visit_date, vrr.records_count,
              COALESCE(rct.name, rc.name) AS category_name_pl
       FROM visit_dates vd
       JOIN visit_record_requirements vrr ON vrr.visit_date_id = vd.id
       JOIN record_categories rc ON rc.id = vrr.record_category_id
       LEFT JOIN record_category_translations rct ON rct.record_category_id = rc.id AND rct.locale = 'pl'
       WHERE vd.student_id = $1
       ORDER BY vd.date ASC`,
      [student.ns_legacy_id]
    );
    timing.pg_query_ms = Date.now() - tQueryStart;
    const visitsMap = /* @__PURE__ */ new Map();
    for (const row of visitResult.rows) {
      if (!visitsMap.has(row.visit_id)) {
        visitsMap.set(row.visit_id, { date: row.visit_date, requirements: [] });
      }
      visitsMap.get(row.visit_id).requirements.push({
        category_name_pl: row.category_name_pl,
        records_count: row.records_count
      });
    }
    visits = [...visitsMap.values()].sort((a, b) => new Date(a.date) - new Date(b.date));
  } finally {
    const tEndStart = Date.now();
    await pgClient.end();
    timing.pg_end_ms = Date.now() - tEndStart;
  }
  const tCatStart = Date.now();
  const uniqueCatNames = [...new Set(
    visits.flatMap((v) => v.requirements.map((r) => r.category_name_pl).filter(Boolean))
  )];
  const catIdMap = {};
  await Promise.all(uniqueCatNames.map(async (name) => {
    const cat = await env.DB.prepare("SELECT id FROM categories WHERE name_pl = ?").bind(name).first();
    if (cat) catIdMap[name] = cat.id;
  }));
  timing.d1_category_lookup_ms = Date.now() - tCatStart;
  let created = 0;
  const skippedCategories = /* @__PURE__ */ new Set();
  let prevVisitTs = null;
  const rows = [];
  for (const visit of visits) {
    const visitTs = Math.floor(new Date(visit.date).getTime() / 1e3);
    const periodStart = prevVisitTs !== null ? prevVisitTs : student.created_at;
    for (const req of visit.requirements) {
      if (!req.category_name_pl) {
        skippedCategories.add("(brak nazwy kategorii)");
        continue;
      }
      const catId = catIdMap[req.category_name_pl];
      if (!catId) {
        skippedCategories.add(req.category_name_pl);
        continue;
      }
      rows.push([catId, req.records_count, periodStart, visitTs]);
      created++;
    }
    prevVisitTs = visitTs;
  }
  const sig = /* @__PURE__ */ __name((a) => a.map((r) => r.map((x) => x == null ? "" : String(x)).join("|")).sort().join(";"), "sig");
  const { results: existingRows } = await env.DB.prepare(
    "SELECT category_id, required_count, period_start, period_end FROM requirements WHERE user_id = ?"
  ).bind(student.id).all();
  const unchanged = sig((existingRows || []).map((r) => [r.category_id, r.required_count, r.period_start, r.period_end])) === sig(rows);
  timing.unchanged = unchanged;
  if (unchanged) {
    timing.total_ms = Date.now() - t0;
    return { created: 0, unchanged: true, visits_found: visits.length, skipped_categories: [...skippedCategories], source: "postgres_hyperdrive", timing };
  }
  const tD1Start = Date.now();
  await env.DB.prepare("DELETE FROM requirements WHERE user_id = ?").bind(student.id).run();
  timing.d1_delete_ms = Date.now() - tD1Start;
  const nowTs = Math.floor(Date.now() / 1e3);
  const stmts = rows.map((r) => env.DB.prepare(
    `INSERT INTO requirements (user_id, category_id, required_count, period_start, period_end, created_at)
     VALUES (?, ?, ?, ?, ?, ?)`
  ).bind(student.id, r[0], r[1], r[2], r[3], nowTs));
  const tBatchStart = Date.now();
  if (stmts.length) {
    const BATCH = 50;
    for (let i = 0; i < stmts.length; i += BATCH) {
      await env.DB.batch(stmts.slice(i, i + BATCH));
    }
  }
  timing.d1_batch_insert_ms = Date.now() - tBatchStart;
  timing.total_ms = Date.now() - t0;
  return { created, visits_found: visits.length, skipped_categories: [...skippedCategories], source: "postgres_hyperdrive", timing };
}
__name(resyncStudentScheduleCore, "resyncStudentScheduleCore");
__name2(resyncStudentScheduleCore, "resyncStudentScheduleCore");
async function handleResyncStudentSchedule(request, env) {
  const body = await request.json();
  const { user_id } = body;
  if (!user_id) return json({ error: "Brak user_id" }, 400);
  const student = await env.DB.prepare("SELECT id, ns_legacy_id, created_at FROM users WHERE id = ?").bind(user_id).first();
  if (!student) return json({ error: "Nie znaleziono kursanta" }, 404);
  if (!student.ns_legacy_id) return json({ error: "Ten kursant nie ma powi\u0105zania z NS (dodany r\u0119cznie?)" }, 400);
  const result = await resyncStudentScheduleCore(env, student);
  return json({ ok: true, ...result });
}
__name(handleResyncStudentSchedule, "handleResyncStudentSchedule");
__name2(handleResyncStudentSchedule, "handleResyncStudentSchedule");
async function handleGetAudio(env, user, recordingId) {
  const rec = await env.DB.prepare(
    `SELECT r.id, r.user_id, r.r2_key, u.group_id FROM recordings r JOIN users u ON r.user_id = u.id WHERE r.id = ?`
  ).bind(recordingId).first();
  if (!rec) return json({ error: "Nie znaleziono nagrania" }, 404);
  if (user.role === "student" && rec.user_id !== user.id) {
    return json({ error: "Brak dost\u0119pu" }, 403);
  }
  if (user.role === "trainer") {
    const access = await env.DB.prepare(
      "SELECT 1 FROM trainer_group_access WHERE trainer_id = ? AND group_id = ?"
    ).bind(user.id, rec.group_id).first();
    if (!access) return json({ error: "Brak dost\u0119pu" }, 403);
  }
  const obj = await env.RECORDINGS.get(rec.r2_key);
  if (!obj) return json({ error: "Plik nie znaleziony w magazynie" }, 404);
  return new Response(obj.body, {
    headers: {
      ...CORS,
      "Content-Type": obj.httpMetadata?.contentType || "audio/webm",
      "Cache-Control": "private, max-age=3600"
    }
  });
}
__name(handleGetAudio, "handleGetAudio");
__name2(handleGetAudio, "handleGetAudio");
async function handleDeleteRecording(env, user, recordingId) {
  const rec = await env.DB.prepare(
    `SELECT r.id, r.user_id, r.r2_key, u.group_id FROM recordings r JOIN users u ON r.user_id = u.id WHERE r.id = ?`
  ).bind(recordingId).first();
  if (!rec) return json({ error: "Nie znaleziono nagrania" }, 404);
  if (user.role === "student" && rec.user_id !== user.id) {
    return json({ error: "Brak dost\u0119pu" }, 403);
  }
  if (user.role === "trainer") {
    const access = await env.DB.prepare(
      "SELECT 1 FROM trainer_group_access WHERE trainer_id = ? AND group_id = ?"
    ).bind(user.id, rec.group_id).first();
    if (!access) return json({ error: "Brak dost\u0119pu" }, 403);
  }
  await env.RECORDINGS.delete(rec.r2_key);
  await env.DB.prepare("DELETE FROM recording_corrections WHERE recording_id = ?").bind(recordingId).run();
  await env.DB.prepare("DELETE FROM recordings WHERE id = ?").bind(recordingId).run();
  return json({ ok: true });
}
__name(handleDeleteRecording, "handleDeleteRecording");
__name2(handleDeleteRecording, "handleDeleteRecording");
async function handleChangeCategory(request, env, user, recordingId) {
  const body = await request.json();
  const { category_key } = body;
  if (!category_key) return json({ error: "Brak kategorii" }, 400);
  const category = await env.DB.prepare("SELECT id FROM categories WHERE key = ?").bind(category_key).first();
  if (!category) return json({ error: "Nieznana kategoria" }, 400);
  const rec = await env.DB.prepare(
    `SELECT r.id, r.user_id, u.group_id FROM recordings r JOIN users u ON r.user_id = u.id WHERE r.id = ?`
  ).bind(recordingId).first();
  if (!rec) return json({ error: "Nie znaleziono nagrania" }, 404);
  if (user.role === "student" && rec.user_id !== user.id) {
    return json({ error: "Brak dost\u0119pu" }, 403);
  }
  if (user.role === "trainer") {
    const access = await env.DB.prepare(
      "SELECT 1 FROM trainer_group_access WHERE trainer_id = ? AND group_id = ?"
    ).bind(user.id, rec.group_id).first();
    if (!access) return json({ error: "Brak dost\u0119pu" }, 403);
  }
  await env.DB.prepare("UPDATE recordings SET category_id = ? WHERE id = ?").bind(category.id, recordingId).run();
  return json({ ok: true });
}
__name(handleChangeCategory, "handleChangeCategory");
__name2(handleChangeCategory, "handleChangeCategory");
async function handleAssignGroup(request, env) {
  const body = await request.json();
  const { email, group_name } = body;
  if (!email || !group_name) return json({ error: "Brak email lub group_name" }, 400);
  let group = await env.DB.prepare("SELECT id FROM groups WHERE name_pl = ?").bind(group_name).first();
  if (!group) {
    const result = await env.DB.prepare(
      "INSERT INTO groups (name_pl, name_en, name_cz) VALUES (?, ?, ?)"
    ).bind(group_name, group_name, group_name).run();
    group = { id: result.meta.last_row_id };
  }
  const user = await env.DB.prepare("SELECT id FROM users WHERE email = ?").bind(email).first();
  if (!user) return json({ error: "Nie znaleziono kursanta o tym emailu" }, 404);
  await env.DB.prepare("UPDATE users SET group_id = ? WHERE id = ?").bind(group.id, user.id).run();
  return json({ ok: true, group_id: group.id });
}
__name(handleAssignGroup, "handleAssignGroup");
__name2(handleAssignGroup, "handleAssignGroup");
async function handleSetRequirementByEmail(request, env) {
  const body = await request.json();
  const { email, category_name, required_count, period_start, period_end } = body;
  if (!email || !category_name || !required_count) {
    return json({ error: "Brak email, category_name lub required_count" }, 400);
  }
  const user = await env.DB.prepare("SELECT id FROM users WHERE email = ?").bind(email).first();
  if (!user) return json({ error: "Nie znaleziono kursanta" }, 404);
  let category = await env.DB.prepare("SELECT id FROM categories WHERE name_pl = ?").bind(category_name).first();
  if (!category) {
    const key = category_name.toLowerCase().replace(/[^a-z0-9]+/g, "_");
    const result2 = await env.DB.prepare(
      "INSERT INTO categories (key, name_pl, name_en, name_cz) VALUES (?, ?, ?, ?)"
    ).bind(key, category_name, category_name, category_name).run();
    category = { id: result2.meta.last_row_id };
  }
  const periodStartTs = period_start ? Math.floor(new Date(period_start).getTime() / 1e3) : null;
  const periodEndTs = period_end ? Math.floor(new Date(period_end).getTime() / 1e3) : null;
  const result = await env.DB.prepare(
    `INSERT INTO requirements (user_id, category_id, required_count, period_start, period_end) VALUES (?, ?, ?, ?, ?)`
  ).bind(user.id, category.id, required_count, periodStartTs, periodEndTs).run();
  return json({ ok: true, requirement_id: result.meta.last_row_id });
}
__name(handleSetRequirementByEmail, "handleSetRequirementByEmail");
__name2(handleSetRequirementByEmail, "handleSetRequirementByEmail");
async function handleStudentDetail(env, trainer, studentId) {
  const student = await env.DB.prepare(
    `SELECT u.*, g.name_pl as group_name
     FROM users u LEFT JOIN groups g ON u.group_id = g.id WHERE u.id = ? AND u.role = 'student'`
  ).bind(studentId).first();
  if (!student) return json({ error: "Nie znaleziono kursanta" }, 404);
  delete student.password_hash;
  const recCount = await env.DB.prepare("SELECT COUNT(*) as cnt FROM recordings WHERE user_id = ?").bind(studentId).first();
  student.total_records_sent = recCount.cnt;
  if (trainer.role !== "admin") {
    const access = await env.DB.prepare(
      "SELECT 1 FROM trainer_group_access WHERE trainer_id = ? AND group_id = ?"
    ).bind(trainer.id, student.group_id).first();
    if (!access) return json({ error: "Brak dost\u0119pu do tego kursanta" }, 403);
  }
  const { results: recordings } = await env.DB.prepare(
    `SELECT id, created_at, status FROM recordings WHERE user_id = ? ORDER BY created_at DESC`
  ).bind(studentId).all();
  const { results: diaryEntries } = await env.DB.prepare(
    "SELECT entry_date, is_correct FROM diary_entries WHERE user_id = ? ORDER BY entry_date DESC"
  ).bind(studentId).all();
  return json({ ok: true, student, recordings, diary_entries: diaryEntries });
}
__name(handleStudentDetail, "handleStudentDetail");
__name2(handleStudentDetail, "handleStudentDetail");
async function handleSaveRatings(request, env, trainer, recordingId) {
  const body = await request.json();
  const { ratings } = body;
  if (!ratings || !ratings.length) return json({ error: "Brak ocen do zapisania" }, 400);
  const rec = await env.DB.prepare(
    `SELECT r.id, r.user_id, u.group_id FROM recordings r JOIN users u ON r.user_id = u.id WHERE r.id = ?`
  ).bind(recordingId).first();
  if (!rec) return json({ error: "Nie znaleziono nagrania" }, 404);
  if (trainer.role !== "admin") {
    const access = await env.DB.prepare(
      "SELECT 1 FROM trainer_group_access WHERE trainer_id = ? AND group_id = ?"
    ).bind(trainer.id, rec.group_id).first();
    if (!access) return json({ error: "Brak dost\u0119pu" }, 403);
  }
  for (const rating of ratings) {
    const criterion = await env.DB.prepare("SELECT id FROM rating_criteria WHERE key = ?").bind(rating.criterion_key).first();
    if (!criterion) continue;
    await env.DB.prepare(
      `INSERT INTO recording_ratings (recording_id, criterion_id, unit_name, percent_mark)
       VALUES (?, ?, ?, ?)
       ON CONFLICT(recording_id, criterion_id) DO UPDATE SET unit_name = excluded.unit_name, percent_mark = excluded.percent_mark`
    ).bind(recordingId, criterion.id, rating.unit_name, rating.percent_mark).run();
  }
  return json({ ok: true });
}
__name(handleSaveRatings, "handleSaveRatings");
__name2(handleSaveRatings, "handleSaveRatings");
async function handleListCriteria(env) {
  const { results } = await env.DB.prepare(
    "SELECT key, name_pl, name_en, name_cz FROM rating_criteria WHERE active = 1 ORDER BY sort_order"
  ).all();
  return json({ ok: true, criteria: results });
}
__name(handleListCriteria, "handleListCriteria");
__name2(handleListCriteria, "handleListCriteria");
async function handleCreateTemplate(request, env, trainer) {
  const body = await request.json();
  const { name, items } = body;
  if (!name || !items || !items.length) return json({ error: "Brak nazwy lub pozycji szablonu" }, 400);
  const tplResult = await env.DB.prepare(
    "INSERT INTO requirement_templates (trainer_id, name) VALUES (?, ?)"
  ).bind(trainer.id, name).run();
  const templateId = tplResult.meta.last_row_id;
  for (const item of items) {
    const category = await env.DB.prepare("SELECT id FROM categories WHERE key = ?").bind(item.category_key).first();
    if (!category) continue;
    await env.DB.prepare(
      "INSERT INTO requirement_template_items (template_id, category_id, required_count) VALUES (?, ?, ?)"
    ).bind(templateId, category.id, item.required_count).run();
  }
  return json({ ok: true, template_id: templateId });
}
__name(handleCreateTemplate, "handleCreateTemplate");
__name2(handleCreateTemplate, "handleCreateTemplate");
async function handleListTemplates(env, trainer) {
  const { results: templates } = await env.DB.prepare(
    "SELECT id, name, created_at FROM requirement_templates WHERE trainer_id = ? ORDER BY created_at DESC"
  ).bind(trainer.id).all();
  for (const tpl of templates) {
    const { results: items } = await env.DB.prepare(
      `SELECT ti.required_count, c.key as category_key, c.name_pl as category_name
       FROM requirement_template_items ti JOIN categories c ON ti.category_id = c.id
       WHERE ti.template_id = ?`
    ).bind(tpl.id).all();
    tpl.items = items;
  }
  return json({ ok: true, templates });
}
__name(handleListTemplates, "handleListTemplates");
__name2(handleListTemplates, "handleListTemplates");
async function handleApplyTemplate(request, env, trainer, templateId) {
  const body = await request.json();
  const { user_id, period_start, period_end } = body;
  if (!user_id) return json({ error: "Brak user_id" }, 400);
  const student = await env.DB.prepare("SELECT group_id FROM users WHERE id = ?").bind(user_id).first();
  if (!student) return json({ error: "Nie znaleziono kursanta" }, 404);
  if (trainer.role !== "admin") {
    const access = await env.DB.prepare(
      "SELECT 1 FROM trainer_group_access WHERE trainer_id = ? AND group_id = ?"
    ).bind(trainer.id, student.group_id).first();
    if (!access) return json({ error: "Brak dost\u0119pu do tego kursanta" }, 403);
  }
  const { results: items } = await env.DB.prepare(
    "SELECT category_id, required_count FROM requirement_template_items WHERE template_id = ?"
  ).bind(templateId).all();
  let applied = 0;
  for (const item of items) {
    await env.DB.prepare(
      "INSERT INTO requirements (user_id, category_id, required_count, period_start, period_end) VALUES (?, ?, ?, ?, ?)"
    ).bind(user_id, item.category_id, item.required_count, period_start || null, period_end || null).run();
    applied++;
  }
  return json({ ok: true, applied });
}
__name(handleApplyTemplate, "handleApplyTemplate");
__name2(handleApplyTemplate, "handleApplyTemplate");
function dedupeRepeatedText(str) {
  if (!str) return str;
  const trimmed = str.trim();
  const len = trimmed.length;
  if (len < 8) return str;
  const half = Math.floor(len / 2);
  const firstHalf = trimmed.slice(0, half).trim();
  const secondHalf = trimmed.slice(len - firstHalf.length).trim();
  return firstHalf.length > 3 && firstHalf === secondHalf ? firstHalf : str;
}
__name(dedupeRepeatedText, "dedupeRepeatedText");
__name2(dedupeRepeatedText, "dedupeRepeatedText");
async function handleSubmitDiary(request, env, user) {
  const body = await request.json();
  const {
    entry_date,
    city,
    success_who,
    talk_no_record,
    phone_no_record,
    control_lost,
    control_details,
    control_context,
    feeling,
    feeling_context,
    hard_situations,
    easy_situations,
    tiredness,
    sleep_hours,
    coffee_count,
    alcohol_types,
    physical_activity,
    motivation,
    meditation_count,
    notes,
    custom_fields
  } = body;
  if (!entry_date) return json({ error: "Brak daty wpisu" }, 400);
  const cleanNotes = dedupeRepeatedText(notes);
  const toJsonOrNull = /* @__PURE__ */ __name2((v) => v === void 0 || v === null ? null : JSON.stringify(v), "toJsonOrNull");
  const result = await env.DB.prepare(
    `INSERT INTO diary_entries
     (user_id, entry_date, city, success_who, talk_no_record, phone_no_record, control_lost,
      control_details, control_context, feeling, feeling_context, hard_situations, easy_situations,
      tiredness, sleep_hours, coffee_count, alcohol_types, physical_activity, motivation,
      meditation_count, notes, custom_fields)
     VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`
  ).bind(
    user.id,
    entry_date,
    city || null,
    toJsonOrNull(success_who),
    toJsonOrNull(talk_no_record),
    toJsonOrNull(phone_no_record),
    control_lost ? 1 : 0,
    toJsonOrNull(control_details),
    control_context || null,
    feeling || null,
    toJsonOrNull(feeling_context),
    toJsonOrNull(hard_situations),
    toJsonOrNull(easy_situations),
    tiredness ?? null,
    sleep_hours ?? null,
    coffee_count ?? null,
    toJsonOrNull(alcohol_types),
    physical_activity ? 1 : 0,
    motivation ?? null,
    meditation_count || 0,
    cleanNotes || null,
    JSON.stringify(custom_fields || {})
  ).run();
  return json({ ok: true, diary_id: result.meta.last_row_id });
}
__name(handleSubmitDiary, "handleSubmitDiary");
__name2(handleSubmitDiary, "handleSubmitDiary");
async function handleGetDiary(request, env, user, overrideUserId) {
  const url = new URL(request.url);
  const targetUserId = overrideUserId != null ? overrideUserId : url.searchParams.get("user_id") ? parseInt(url.searchParams.get("user_id")) : user.id;
  const limit = Math.max(1, Math.min(parseInt(url.searchParams.get("limit") || "10") || 10, 400));
  const fromDate = /^\d{4}-\d{2}-\d{2}$/.test(url.searchParams.get("from") || "") ? url.searchParams.get("from") : null;
  if (user.role === "student" && targetUserId !== user.id) {
    return json({ error: "Brak uprawnie\u0144" }, 403);
  }
  if (user.role === "trainer") {
    const student = await env.DB.prepare("SELECT group_id FROM users WHERE id = ?").bind(targetUserId).first();
    if (!student) return json({ error: "Nie znaleziono kursanta" }, 404);
    const access = await env.DB.prepare(
      "SELECT 1 FROM trainer_group_access WHERE trainer_id = ? AND group_id = ?"
    ).bind(user.id, student.group_id).first();
    if (!access) return json({ error: "Brak dost\u0119pu" }, 403);
  }
  const { results } = fromDate ? await env.DB.prepare(
    "SELECT * FROM diary_entries WHERE user_id = ? AND entry_date >= ? ORDER BY entry_date DESC LIMIT ?"
  ).bind(targetUserId, fromDate, limit).all() : await env.DB.prepare(
    "SELECT * FROM diary_entries WHERE user_id = ? ORDER BY entry_date DESC LIMIT ?"
  ).bind(targetUserId, limit).all();
  return json({ ok: true, entries: results });
}
__name(handleGetDiary, "handleGetDiary");
__name2(handleGetDiary, "handleGetDiary");
async function handleUnreadDiaryFromNs(env, user) {
  const isAdmin = user.role === "admin";
  const groupFilter = isAdmin ? "1=1" : "u.group_id IN (SELECT group_id FROM trainer_group_access WHERE trainer_id = ?)";
  const bindArg = isAdmin ? [] : [user.id];
  const { results } = await env.DB.prepare(
    `SELECT d.id as diary_id, u.id as student_id, u.ns_legacy_id as ns_student_id, u.name as student_name, d.entry_date
     FROM diary_entries d JOIN users u ON d.user_id = u.id
     WHERE d.trainer_read_at IS NULL AND ${groupFilter}
       AND d.id = (SELECT MAX(d2.id) FROM diary_entries d2 WHERE d2.user_id = d.user_id AND d2.entry_date = d.entry_date AND d2.trainer_read_at IS NULL)
     ORDER BY d.entry_date DESC LIMIT 5000`
  ).bind(...bindArg).all();
  return json({ ok: true, unread_diary_entries: results });
}
__name(handleUnreadDiaryFromNs, "handleUnreadDiaryFromNs");
__name2(handleUnreadDiaryFromNs, "handleUnreadDiaryFromNs");
async function handleDashboard(env, user) {
  const isAdmin = user.role === "admin";
  const groupFilter = isAdmin ? "1=1" : "u.group_id IN (SELECT group_id FROM trainer_group_access WHERE trainer_id = ?)";
  const bindArg = isAdmin ? [] : [user.id];
  const { results: pendingList } = await env.DB.prepare(
    `SELECT r.id as recording_id, u.id as student_id, u.name as student_name, c.name_pl as category_name, r.created_at, r.status
     FROM recordings r JOIN users u ON r.user_id = u.id JOIN categories c ON r.category_id = c.id
     WHERE (r.status = 'pending' OR (r.status = 'needs_improvement' AND r.id NOT IN (SELECT recording_id FROM recording_corrections)))
       AND ${groupFilter} ORDER BY r.created_at DESC LIMIT 50`
  ).bind(...bindArg).all();
  const { results: diaryUnreadList } = await env.DB.prepare(
    `SELECT d.id as diary_id, u.id as student_id, u.name as student_name, d.entry_date
     FROM diary_entries d JOIN users u ON d.user_id = u.id
     WHERE d.trainer_read_at IS NULL AND ${groupFilter}
       AND d.id = (SELECT MAX(d2.id) FROM diary_entries d2 WHERE d2.user_id = d.user_id AND d2.entry_date = d.entry_date AND d2.trainer_read_at IS NULL)
     ORDER BY d.entry_date DESC LIMIT 50`
  ).bind(...bindArg).all();
  const nowTs = Math.floor(Date.now() / 1e3);
  const { results: activeSchedules } = await env.DB.prepare(
    `SELECT u.id as student_id, u.name as student_name,
       SUM(r.required_count) as total_required,
       SUM(MIN(r.required_count, COALESCE(rc.cnt, 0))) as total_correct_capped,
       MIN(r.period_start) as period_start, MAX(r.period_end) as period_end
     FROM requirements r
     JOIN users u ON r.user_id = u.id
     LEFT JOIN (
       SELECT rec.user_id as uid, rec.category_id as cid, rq.period_start as pstart, rq.period_end as pend, COUNT(*) as cnt
       FROM recordings rec
       JOIN requirements rq ON rq.user_id = rec.user_id AND rq.category_id = rec.category_id
         AND rec.created_at >= rq.period_start AND rec.created_at <= rq.period_end
       WHERE rec.status='correct'
       GROUP BY rec.user_id, rec.category_id, rq.period_start, rq.period_end
     ) rc ON rc.uid = r.user_id AND rc.cid = r.category_id
       AND rc.pstart = r.period_start AND rc.pend = r.period_end
     WHERE ${groupFilter} AND r.period_start <= ? AND r.period_end >= ?
     GROUP BY u.id, u.name
     ORDER BY u.name LIMIT 100`
  ).bind(...bindArg, nowTs, nowTs).all();
  const studentsResult = await env.DB.prepare(
    isAdmin ? `SELECT COUNT(*) as cnt FROM users WHERE role='student' AND active=1` : `SELECT COUNT(*) as cnt FROM users WHERE role='student' AND active=1 AND group_id IN (SELECT group_id FROM trainer_group_access WHERE trainer_id = ?)`
  ).bind(...isAdmin ? [] : [user.id]).first();
  return json({
    ok: true,
    pending_recordings: pendingList,
    unread_diary_entries: diaryUnreadList,
    active_schedules: activeSchedules.map((s) => ({
      student_id: s.student_id,
      student_name: s.student_name,
      pct: s.total_required > 0 ? Math.round(s.total_correct_capped / s.total_required * 100) : 0,
      period_start: s.period_start,
      period_end: s.period_end
    })),
    active_students_count: studentsResult.cnt
  });
}
__name(handleDashboard, "handleDashboard");
__name2(handleDashboard, "handleDashboard");
async function handleUpdateProfileByEmail(request, env) {
  const body = await request.json();
  const { email, ...fields } = body;
  if (!email) return json({ error: "Brak email" }, 400);
  const user = await env.DB.prepare("SELECT id FROM users WHERE email = ?").bind(email).first();
  if (!user) return json({ error: "Nie znaleziono kursanta" }, 404);
  const editable = [
    "first_name",
    "last_name",
    "phone",
    "city",
    "country",
    "birthdate",
    "facebook_profile",
    "therapy_beginning",
    "therapy_end",
    "my_history",
    "my_history_public",
    "my_nowamowa",
    "my_nowamowa_public",
    "diary_permission",
    "records_permission",
    "max_records_count",
    "max_inactivity_unit",
    "max_inactivity_value",
    "record_target_period",
    "record_target_count",
    "diary_supervision_end",
    "record_supervision_end",
    "diagnosis_id",
    "is_diagnosis_paid",
    "is_diagnosis_done",
    "blocked_at"
  ];
  const setClauses = [];
  const values = [];
  for (const field of editable) {
    if (fields.hasOwnProperty(field)) {
      setClauses.push(`${field} = ?`);
      values.push(fields[field]);
    }
  }
  if (!setClauses.length) return json({ error: "Brak p\xF3l do zaktualizowania" }, 400);
  values.push(user.id);
  await env.DB.prepare(`UPDATE users SET ${setClauses.join(", ")} WHERE id = ?`).bind(...values).run();
  return json({ ok: true });
}
__name(handleUpdateProfileByEmail, "handleUpdateProfileByEmail");
__name2(handleUpdateProfileByEmail, "handleUpdateProfileByEmail");
async function handleImportRecordingMeta(request, env) {
  const body = await request.json();
  const { email, category_name, ns_record_id, status, duration_sec, note, created_at } = body;
  if (!email || !category_name || !ns_record_id) return json({ error: "Brak email/kategorii/ns_record_id" }, 400);
  const user = await env.DB.prepare("SELECT id, slug FROM users WHERE email = ?").bind(email).first();
  if (!user) return json({ error: "Nie znaleziono kursanta" }, 404);
  const categoryKey = category_name.toLowerCase().replace(/[^a-z0-9]+/g, "_");
  let category = await env.DB.prepare("SELECT id FROM categories WHERE key = ?").bind(categoryKey).first();
  if (!category) {
    const result2 = await env.DB.prepare(
      "INSERT INTO categories (key, name_pl, name_en, name_cz) VALUES (?, ?, ?, ?)"
    ).bind(categoryKey, category_name, category_name, category_name).run();
    category = { id: result2.meta.last_row_id };
  }
  const r2Key = `_no_audio_imported/${user.slug}/${ns_record_id}`;
  const existing = await env.DB.prepare("SELECT id FROM recordings WHERE r2_key = ?").bind(r2Key).first();
  if (existing) return json({ ok: true, skipped: true });
  const createdTs = created_at ? Math.floor(new Date(created_at).getTime() / 1e3) : Math.floor(Date.now() / 1e3);
  const mappedStatus = status === true || status === "true" ? "correct" : status === false || status === "false" ? "needs_improvement" : "pending";
  const result = await env.DB.prepare(
    `INSERT INTO recordings (user_id, category_id, r2_key, filename, duration_sec, note, status, created_at)
     VALUES (?, ?, ?, ?, ?, ?, ?, ?)`
  ).bind(user.id, category.id, r2Key, "(historyczny import \u2014 bez pliku audio)", duration_sec || null, note || null, mappedStatus, createdTs).run();
  return json({ ok: true, recording_id: result.meta.last_row_id });
}
__name(handleImportRecordingMeta, "handleImportRecordingMeta");
__name2(handleImportRecordingMeta, "handleImportRecordingMeta");
async function handleImportRatings(request, env) {
  const body = await request.json();
  const { recording_id, ratings } = body;
  if (!recording_id || !ratings) return json({ error: "Brak recording_id lub ratings" }, 400);
  for (const rating of ratings) {
    let criterion = await env.DB.prepare("SELECT id FROM rating_criteria WHERE key = ?").bind(rating.criterion_key).first();
    if (!criterion) {
      const result2 = await env.DB.prepare(
        "INSERT INTO rating_criteria (key, name_pl, name_en, name_cz, sort_order) VALUES (?, ?, ?, ?, 99)"
      ).bind(rating.criterion_key, rating.criterion_name || rating.criterion_key, rating.criterion_name || rating.criterion_key, rating.criterion_name || rating.criterion_key).run();
      criterion = { id: result2.meta.last_row_id };
    }
    await env.DB.prepare(
      `INSERT INTO recording_ratings (recording_id, criterion_id, unit_name, percent_mark)
       VALUES (?, ?, ?, ?)
       ON CONFLICT(recording_id, criterion_id) DO UPDATE SET unit_name = excluded.unit_name, percent_mark = excluded.percent_mark`
    ).bind(recording_id, criterion.id, rating.unit_name, rating.percent_mark).run();
  }
  return json({ ok: true });
}
__name(handleImportRatings, "handleImportRatings");
__name2(handleImportRatings, "handleImportRatings");
async function handleImportRecording(request, env) {
  const formData = await request.formData();
  const email = formData.get("original_student_email");
  const file = formData.get("file");
  const categoryKey = formData.get("category_key");
  if (!email || !file || !categoryKey) return json({ error: "Brak email/pliku/kategorii" }, 400);
  const user = await env.DB.prepare("SELECT id, slug FROM users WHERE email = ?").bind(email).first();
  if (!user) return json({ error: "Nie znaleziono kursanta" }, 404);
  let category = await env.DB.prepare("SELECT id FROM categories WHERE key = ?").bind(categoryKey).first();
  if (!category) {
    const result2 = await env.DB.prepare(
      "INSERT INTO categories (key, name_pl, name_en, name_cz) VALUES (?, ?, ?, ?)"
    ).bind(categoryKey, categoryKey, categoryKey, categoryKey).run();
    category = { id: result2.meta.last_row_id };
  }
  const durationSec = parseFloat(formData.get("duration_sec")) || null;
  const note = formData.get("note") || null;
  const ts = Date.now() + Math.floor(Math.random() * 1e3);
  const r2Key = `${user.slug}/${categoryKey}/imported_${ts}.mp3`;
  await env.RECORDINGS.put(r2Key, await file.arrayBuffer(), { httpMetadata: { contentType: "audio/mpeg" } });
  const result = await env.DB.prepare(
    `INSERT INTO recordings (user_id, category_id, r2_key, filename, duration_sec, note, status, created_at)
     VALUES (?, ?, ?, ?, ?, ?, 'correct', ?)`
  ).bind(user.id, category.id, r2Key, "imported.mp3", durationSec, note, Math.floor(Date.now() / 1e3)).run();
  return json({ ok: true, recording_id: result.meta.last_row_id });
}
__name(handleImportRecording, "handleImportRecording");
__name2(handleImportRecording, "handleImportRecording");
async function handleImportDiary(request, env) {
  const body = await request.json();
  const { email, entry } = body;
  if (!email || !entry) return json({ error: "Brak email lub entry" }, 400);
  const user = await env.DB.prepare("SELECT id FROM users WHERE email = ?").bind(email).first();
  if (!user) return json({ error: "Nie znaleziono kursanta" }, 404);
  const entryDate = entry.date || entry.entry_date || entry.created_at || (/* @__PURE__ */ new Date()).toISOString().slice(0, 10);
  const knownFields = {
    feeling: entry.feeling || entry.mood || null,
    notes: dedupeRepeatedText(entry.notes || entry.note || entry.description || null),
    sleep_hours: entry.sleep_hours || entry.sleep || null,
    tiredness: entry.tiredness || entry.fatigue || null,
    motivation: entry.motivation || null
  };
  const result = await env.DB.prepare(
    `INSERT INTO diary_entries (user_id, entry_date, feeling, notes, sleep_hours, tiredness, motivation, custom_fields)
     VALUES (?, ?, ?, ?, ?, ?, ?, ?)`
  ).bind(
    user.id,
    String(entryDate).slice(0, 10),
    knownFields.feeling,
    knownFields.notes,
    knownFields.sleep_hours,
    knownFields.tiredness,
    knownFields.motivation,
    JSON.stringify(entry)
    // pełny oryginalny obiekt zachowany na wszelki wypadek
  ).run();
  return json({ ok: true, diary_id: result.meta.last_row_id });
}
__name(handleImportDiary, "handleImportDiary");
__name2(handleImportDiary, "handleImportDiary");
async function handleIncrementalImportRecordings(request, env) {
  const body = await request.json();
  const { recordings } = body;
  if (!recordings || !recordings.length) return json({ error: "Brak recordings" }, 400);
  let imported = 0, skipped = 0;
  const stmts = [];
  for (const rec of recordings) {
    const user = await env.DB.prepare("SELECT id FROM users WHERE ns_legacy_id = ?").bind(rec.ns_user_id).first();
    const cat = await env.DB.prepare("SELECT id FROM categories WHERE name_pl = ?").bind(rec.category_name).first();
    if (!user || !cat) {
      skipped++;
      continue;
    }
    const r2Key = `_ns_import/${rec.ns_record_id}`;
    const createdTs = rec.created_at ? Math.floor(new Date(rec.created_at).getTime() / 1e3) : Math.floor(Date.now() / 1e3);
    const mappedStatus = rec.status === true ? "correct" : rec.status === false ? "needs_improvement" : "pending";
    stmts.push(env.DB.prepare(
      `INSERT INTO recordings (user_id, category_id, r2_key, filename, note, status, created_at, ns_legacy_id)
       VALUES (?, ?, ?, '(import z bazy NS)', ?, ?, ?, ?)
       ON CONFLICT(r2_key) DO NOTHING`
    ).bind(user.id, cat.id, r2Key, rec.note || null, mappedStatus, createdTs, rec.ns_record_id));
  }
  const BATCH_SIZE = 50;
  for (let i = 0; i < stmts.length; i += BATCH_SIZE) {
    const result = await env.DB.batch(stmts.slice(i, i + BATCH_SIZE));
    imported += result.filter((r) => r.meta.changes > 0).length;
  }
  return json({ ok: true, imported, skipped });
}
__name(handleIncrementalImportRecordings, "handleIncrementalImportRecordings");
__name2(handleIncrementalImportRecordings, "handleIncrementalImportRecordings");
async function handleIncrementalImportDiary(request, env) {
  const body = await request.json();
  const { entries } = body;
  if (!entries || !entries.length) return json({ error: "Brak entries" }, 400);
  let imported = 0, skipped = 0;
  const stmts = [];
  for (const entry of entries) {
    const user = await env.DB.prepare("SELECT id FROM users WHERE ns_legacy_id = ?").bind(entry.ns_user_id).first();
    if (!user) {
      skipped++;
      continue;
    }
    const entryDate = String(entry.date || "").slice(0, 10);
    if (!entryDate) {
      skipped++;
      continue;
    }
    stmts.push(env.DB.prepare(
      `INSERT INTO diary_entries (user_id, entry_date, notes, custom_fields)
       SELECT ?, ?, ?, ?
       WHERE NOT EXISTS (SELECT 1 FROM diary_entries WHERE user_id = ? AND entry_date = ?)`
    ).bind(user.id, entryDate, dedupeRepeatedText(entry.notes) || null, JSON.stringify(entry), user.id, entryDate));
  }
  const BATCH_SIZE = 50;
  for (let i = 0; i < stmts.length; i += BATCH_SIZE) {
    const result = await env.DB.batch(stmts.slice(i, i + BATCH_SIZE));
    imported += result.filter((r) => r.meta.changes > 0).length;
  }
  return json({ ok: true, imported, skipped });
}
__name(handleIncrementalImportDiary, "handleIncrementalImportDiary");
__name2(handleIncrementalImportDiary, "handleIncrementalImportDiary");
function rateKeyFromName(name) {
  const n = (name || "").toLowerCase();
  if (n.startsWith("spok")) return "spokoj";
  if (n.startsWith("rezonans")) return "rezonans";
  if (n.includes("ekki") && n.includes("ydech")) return "lekki_wydech";
  if (n.startsWith("intonacj")) return "intonacja";
  if (n.startsWith("ogonek")) return "ogonek";
  if (n.startsWith("tempo")) return "tempo";
  if (n.startsWith("pauzy")) return "pauzy";
  if (n.startsWith("zlepki") || n.includes("rzygotow")) return "zlepki";
  return (name || "").toLowerCase().replace(/[^a-z0-9ąćęłńóśźż]+/g, "_").replace(/^_|_$/g, "");
}
__name(rateKeyFromName, "rateKeyFromName");
__name2(rateKeyFromName, "rateKeyFromName");
async function handleIncrementalImportRatings(request, env) {
  const body = await request.json();
  const { ratings } = body;
  if (!ratings || !ratings.length) return json({ error: "Brak ratings" }, 400);
  let imported = 0, skipped = 0;
  const stmts = [];
  for (const r of ratings) {
    const recording = await env.DB.prepare("SELECT id FROM recordings WHERE ns_legacy_id = ?").bind(r.ns_record_id).first();
    if (!recording) {
      skipped++;
      continue;
    }
    const critKey = rateKeyFromName(r.criterion_name);
    const criterion = await env.DB.prepare("SELECT id FROM rating_criteria WHERE key = ?").bind(critKey).first();
    if (!criterion) {
      skipped++;
      continue;
    }
    stmts.push(env.DB.prepare(
      `INSERT INTO recording_ratings (recording_id, criterion_id, unit_name, percent_mark)
       VALUES (?, ?, ?, ?)
       ON CONFLICT(recording_id, criterion_id) DO UPDATE SET unit_name=excluded.unit_name, percent_mark=excluded.percent_mark`
    ).bind(recording.id, criterion.id, r.unit_name || null, r.percent_mark || 0));
  }
  const BATCH_SIZE = 50;
  for (let i = 0; i < stmts.length; i += BATCH_SIZE) {
    const result = await env.DB.batch(stmts.slice(i, i + BATCH_SIZE));
    imported += result.filter((r) => r.meta.changes > 0).length;
  }
  return json({ ok: true, imported, skipped });
}
__name(handleIncrementalImportRatings, "handleIncrementalImportRatings");
__name2(handleIncrementalImportRatings, "handleIncrementalImportRatings");
async function handleImportRecordingsBulk(request, env) {
  const body = await request.json();
  const { email, recordings } = body;
  if (!email || !recordings || !recordings.length) return json({ error: "Brak email lub recordings" }, 400);
  const user = await env.DB.prepare("SELECT id, slug FROM users WHERE email = ?").bind(email).first();
  if (!user) return json({ error: "Nie znaleziono kursanta" }, 404);
  const uniqueCats = [...new Set(recordings.map((r) => r.category_name).filter(Boolean))];
  const catIdMap = {};
  for (const catName of uniqueCats) {
    const key = catName.toLowerCase().replace(/[^a-z0-9]+/g, "_");
    let cat = await env.DB.prepare("SELECT id FROM categories WHERE key = ?").bind(key).first();
    if (!cat) {
      const result = await env.DB.prepare(
        "INSERT INTO categories (key, name_pl, name_en, name_cz) VALUES (?, ?, ?, ?)"
      ).bind(key, catName, catName, catName).run();
      cat = { id: result.meta.last_row_id };
    }
    catIdMap[catName] = cat.id;
  }
  const stmts = [];
  const nsIdOrder = [];
  for (const rec of recordings) {
    const catId = catIdMap[rec.category_name];
    if (!catId) continue;
    const r2Key = `_no_audio_imported/${user.slug}/${rec.ns_record_id}`;
    const createdTs = rec.created_at ? Math.floor(new Date(rec.created_at).getTime() / 1e3) : Math.floor(Date.now() / 1e3);
    const mappedStatus = rec.status === true ? "correct" : rec.status === false ? "needs_improvement" : "pending";
    nsIdOrder.push(rec.ns_record_id);
    stmts.push(env.DB.prepare(
      `INSERT INTO recordings (user_id, category_id, r2_key, filename, duration_sec, note, status, created_at)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?)
       ON CONFLICT(r2_key) DO UPDATE SET status=excluded.status
       RETURNING id`
    ).bind(user.id, catId, r2Key, "(historyczny import \u2014 bez pliku audio)", rec.duration_sec || null, rec.note || null, mappedStatus, createdTs));
  }
  if (!stmts.length) return json({ ok: true, imported: 0 });
  const idMap = {};
  let importedCount = 0;
  const BATCH_SIZE = 50;
  for (let i = 0; i < stmts.length; i += BATCH_SIZE) {
    const chunkStmts = stmts.slice(i, i + BATCH_SIZE);
    const chunkNsIds = nsIdOrder.slice(i, i + BATCH_SIZE);
    const results = await env.DB.batch(chunkStmts);
    results.forEach((r, j) => {
      if (r.results && r.results[0] && r.results[0].id) {
        idMap[chunkNsIds[j]] = r.results[0].id;
        importedCount++;
      }
    });
  }
  return json({ ok: true, imported: importedCount, id_map: idMap });
}
__name(handleImportRecordingsBulk, "handleImportRecordingsBulk");
__name2(handleImportRecordingsBulk, "handleImportRecordingsBulk");
async function handleSetRequirementsBulk(request, env) {
  const body = await request.json();
  const { email, requirements } = body;
  if (!email || !requirements || !requirements.length) return json({ error: "Brak email lub requirements" }, 400);
  const user = await env.DB.prepare("SELECT id FROM users WHERE email = ?").bind(email).first();
  if (!user) return json({ error: "Nie znaleziono kursanta" }, 404);
  const uniqueCats = [...new Set(requirements.map((r) => r.category_name).filter(Boolean))];
  const catIdMap = {};
  for (const catName of uniqueCats) {
    let cat = await env.DB.prepare("SELECT id FROM categories WHERE name_pl = ?").bind(catName).first();
    if (!cat) {
      const key = catName.toLowerCase().replace(/[^a-z0-9]+/g, "_");
      const result = await env.DB.prepare(
        "INSERT INTO categories (key, name_pl, name_en, name_cz) VALUES (?, ?, ?, ?)"
      ).bind(key, catName, catName, catName).run();
      cat = { id: result.meta.last_row_id };
    }
    catIdMap[catName] = cat.id;
  }
  const stmts = [];
  for (const req of requirements) {
    const catId = catIdMap[req.category_name];
    if (!catId || !req.required_count) continue;
    const periodStartTs = req.period_start ? Math.floor(new Date(req.period_start).getTime() / 1e3) : null;
    const periodEndTs = req.period_end ? Math.floor(new Date(req.period_end).getTime() / 1e3) : null;
    stmts.push(env.DB.prepare(
      `INSERT INTO requirements (user_id, category_id, required_count, period_start, period_end) VALUES (?, ?, ?, ?, ?)`
    ).bind(user.id, catId, req.required_count, periodStartTs, periodEndTs));
  }
  if (!stmts.length) return json({ ok: true, imported: 0 });
  const BATCH_SIZE = 50;
  for (let i = 0; i < stmts.length; i += BATCH_SIZE) {
    await env.DB.batch(stmts.slice(i, i + BATCH_SIZE));
  }
  return json({ ok: true, imported: stmts.length });
}
__name(handleSetRequirementsBulk, "handleSetRequirementsBulk");
__name2(handleSetRequirementsBulk, "handleSetRequirementsBulk");
async function handleImportDiaryBulk(request, env) {
  const body = await request.json();
  const { email, entries } = body;
  if (!email || !entries || !entries.length) return json({ error: "Brak email lub entries" }, 400);
  const user = await env.DB.prepare("SELECT id FROM users WHERE email = ?").bind(email).first();
  if (!user) return json({ error: "Nie znaleziono kursanta" }, 404);
  const stmts = entries.map((entry) => {
    const entryDate = entry.date || entry.entry_date || entry.created_at || (/* @__PURE__ */ new Date()).toISOString().slice(0, 10);
    const feeling = entry.feeling || entry.mood || null;
    const notes = dedupeRepeatedText(entry.notes || entry.note || entry.description || null);
    const sleepHours = entry.sleep_hours || entry.sleep || null;
    const tiredness = entry.tiredness || entry.fatigue || null;
    const motivation = entry.motivation || null;
    return env.DB.prepare(
      `INSERT INTO diary_entries (user_id, entry_date, feeling, notes, sleep_hours, tiredness, motivation, custom_fields)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?)`
    ).bind(user.id, String(entryDate).slice(0, 10), feeling, notes, sleepHours, tiredness, motivation, JSON.stringify(entry));
  });
  const BATCH_SIZE = 50;
  for (let i = 0; i < stmts.length; i += BATCH_SIZE) {
    await env.DB.batch(stmts.slice(i, i + BATCH_SIZE));
  }
  return json({ ok: true, imported: stmts.length });
}
__name(handleImportDiaryBulk, "handleImportDiaryBulk");
__name2(handleImportDiaryBulk, "handleImportDiaryBulk");
async function handleImportRatingsBulk(request, env) {
  const body = await request.json();
  const { items } = body;
  if (!items || !items.length) return json({ ok: true, imported: 0 });
  const allCriterionKeys = /* @__PURE__ */ new Set();
  items.forEach((it) => (it.ratings || []).forEach((r) => allCriterionKeys.add(r.criterion_key)));
  const critIdMap = {};
  for (const key of allCriterionKeys) {
    let crit = await env.DB.prepare("SELECT id FROM rating_criteria WHERE key = ?").bind(key).first();
    if (!crit) {
      const sample = items.flatMap((it) => it.ratings || []).find((r) => r.criterion_key === key);
      const result = await env.DB.prepare(
        "INSERT INTO rating_criteria (key, name_pl, name_en, name_cz, sort_order) VALUES (?, ?, ?, ?, 99)"
      ).bind(key, sample && sample.criterion_name || key, sample && sample.criterion_name || key, sample && sample.criterion_name || key).run();
      crit = { id: result.meta.last_row_id };
    }
    critIdMap[key] = crit.id;
  }
  const stmts = [];
  for (const item of items) {
    for (const r of item.ratings || []) {
      const critId = critIdMap[r.criterion_key];
      if (!critId) continue;
      stmts.push(env.DB.prepare(
        `INSERT INTO recording_ratings (recording_id, criterion_id, unit_name, percent_mark)
         VALUES (?, ?, ?, ?)
         ON CONFLICT(recording_id, criterion_id) DO UPDATE SET unit_name = excluded.unit_name, percent_mark = excluded.percent_mark`
      ).bind(item.recording_id, critId, r.unit_name, r.percent_mark));
    }
  }
  if (!stmts.length) return json({ ok: true, imported: 0 });
  const BATCH_SIZE = 50;
  for (let i = 0; i < stmts.length; i += BATCH_SIZE) {
    await env.DB.batch(stmts.slice(i, i + BATCH_SIZE));
  }
  return json({ ok: true, imported: stmts.length });
}
__name(handleImportRatingsBulk, "handleImportRatingsBulk");
__name2(handleImportRatingsBulk, "handleImportRatingsBulk");
async function handleStudentDay(env, trainer, studentId, dateStr) {
  const student = await env.DB.prepare("SELECT group_id FROM users WHERE id = ?").bind(studentId).first();
  if (!student) return json({ error: "Nie znaleziono kursanta" }, 404);
  if (trainer.role !== "admin") {
    const access = await env.DB.prepare("SELECT 1 FROM trainer_group_access WHERE trainer_id = ? AND group_id = ?").bind(trainer.id, student.group_id).first();
    if (!access) return json({ error: "Brak dost\u0119pu" }, 403);
  }
  const dayStart = Math.floor((/* @__PURE__ */ new Date(dateStr + "T00:00:00")).getTime() / 1e3);
  const dayEnd = dayStart + 86400;
  const { results: recordings } = await env.DB.prepare(
    `SELECT r.*, c.key as category_key, c.name_pl as category_name
     FROM recordings r JOIN categories c ON r.category_id = c.id
     WHERE r.user_id = ? AND r.created_at >= ? AND r.created_at < ? ORDER BY r.created_at ASC`
  ).bind(studentId, dayStart, dayEnd).all();
  for (const rec of recordings) {
    const { results: ratings } = await env.DB.prepare(
      `SELECT rr.unit_name, rr.percent_mark, rc.name_pl as criterion_name, rc.key as criterion_key
       FROM recording_ratings rr JOIN rating_criteria rc ON rr.criterion_id = rc.id
       WHERE rr.recording_id = ? ORDER BY rc.sort_order`
    ).bind(rec.id).all();
    rec.ratings = ratings;
    const correction = await env.DB.prepare(
      "SELECT id FROM recording_corrections WHERE recording_id = ? LIMIT 1"
    ).bind(rec.id).first();
    rec.has_correction = !!correction;
  }
  const diaryEntry = await env.DB.prepare(
    "SELECT * FROM diary_entries WHERE user_id = ? AND entry_date = ?"
  ).bind(studentId, dateStr).first();
  return json({ ok: true, date: dateStr, recordings, diary_entry: diaryEntry || null });
}
__name(handleStudentDay, "handleStudentDay");
__name2(handleStudentDay, "handleStudentDay");
async function handleStudentStats(env, trainer, studentId) {
  const student = await env.DB.prepare("SELECT group_id FROM users WHERE id = ?").bind(studentId).first();
  if (!student) return json({ error: "Nie znaleziono kursanta" }, 404);
  if (trainer.role !== "admin") {
    const access = await env.DB.prepare("SELECT 1 FROM trainer_group_access WHERE trainer_id = ? AND group_id = ?").bind(trainer.id, student.group_id).first();
    if (!access) return json({ error: "Brak dost\u0119pu" }, 403);
  }
  const totals = await env.DB.prepare(
    `SELECT COUNT(*) as total,
       SUM(CASE WHEN status='correct' THEN 1 ELSE 0 END) as correct,
       SUM(CASE WHEN status='needs_improvement' THEN 1 ELSE 0 END) as needs_improvement,
       SUM(CASE WHEN status='pending' THEN 1 ELSE 0 END) as pending,
       SUM(CASE WHEN status='auto_flagged' THEN 1 ELSE 0 END) as auto_flagged
     FROM recordings WHERE user_id = ?`
  ).bind(studentId).first();
  const { results: byCategory } = await env.DB.prepare(
    `SELECT c.name_pl as category_name, COUNT(*) as total,
       SUM(CASE WHEN r.status='correct' THEN 1 ELSE 0 END) as correct
     FROM recordings r JOIN categories c ON r.category_id = c.id
     WHERE r.user_id = ? GROUP BY c.id ORDER BY total DESC`
  ).bind(studentId).all();
  const eightWeeksAgo = Math.floor(Date.now() / 1e3) - 56 * 24 * 60 * 60;
  const { results: recentRecordings } = await env.DB.prepare(
    `SELECT created_at FROM recordings WHERE user_id = ? AND created_at > ? ORDER BY created_at ASC`
  ).bind(studentId, eightWeeksAgo).all();
  const weekBuckets = {};
  recentRecordings.forEach((r) => {
    const weekStart = Math.floor(r.created_at / (7 * 24 * 60 * 60));
    weekBuckets[weekStart] = (weekBuckets[weekStart] || 0) + 1;
  });
  const weeklyTrend = Object.keys(weekBuckets).sort().map((w) => ({ week: parseInt(w), count: weekBuckets[w] }));
  const avgRating = await env.DB.prepare(
    `SELECT AVG(percent_mark) as avg_pct FROM recording_ratings rr
     JOIN recordings r ON rr.recording_id = r.id WHERE r.user_id = ?`
  ).bind(studentId).first();
  return json({
    ok: true,
    totals: { total: totals.total || 0, correct: totals.correct || 0, needs_improvement: totals.needs_improvement || 0, pending: totals.pending || 0, auto_flagged: totals.auto_flagged || 0 },
    by_category: byCategory,
    weekly_trend: weeklyTrend,
    avg_rating_pct: avgRating.avg_pct ? Math.round(avgRating.avg_pct) : null
  });
}
__name(handleStudentStats, "handleStudentStats");
__name2(handleStudentStats, "handleStudentStats");
function slugify(name) {
  const map = {
    "\u0105": "a",
    "\u0107": "c",
    "\u0119": "e",
    "\u0142": "l",
    "\u0144": "n",
    "\xF3": "o",
    "\u015B": "s",
    "\u017A": "z",
    "\u017C": "z",
    "\u0104": "A",
    "\u0106": "C",
    "\u0118": "E",
    "\u0141": "L",
    "\u0143": "N",
    "\xD3": "O",
    "\u015A": "S",
    "\u0179": "Z",
    "\u017B": "Z",
    "\xE1": "a",
    "\u010D": "c",
    "\u010F": "d",
    "\xE9": "e",
    "\u011B": "e",
    "\xED": "i",
    "\u0148": "n",
    "\u0159": "r",
    "\u0161": "s",
    "\u0165": "t",
    "\xFA": "u",
    "\u016F": "u",
    "\xFD": "y",
    "\u017E": "z",
    "\xC1": "A",
    "\u010C": "C",
    "\u010E": "D",
    "\xC9": "E",
    "\u011A": "E",
    "\xCD": "I",
    "\u0147": "N",
    "\u0158": "R",
    "\u0160": "S",
    "\u0164": "T",
    "\xDA": "U",
    "\u016E": "U",
    "\xDD": "Y",
    "\u017D": "Z"
  };
  let result = name.split("").map((ch) => map[ch] || ch).join("");
  return result.toLowerCase().normalize("NFD").replace(/[\u0300-\u036f]/g, "").replace(/[^a-z0-9]+/g, "-").replace(/^-+|-+$/g, "");
}
__name(slugify, "slugify");
__name2(slugify, "slugify");
function generateToken() {
  const arr = new Uint8Array(32);
  crypto.getRandomValues(arr);
  return Array.from(arr).map((b) => b.toString(16).padStart(2, "0")).join("");
}
__name(generateToken, "generateToken");
__name2(generateToken, "generateToken");
async function hashPassword(password, salt) {
  const enc = new TextEncoder();
  const keyMaterial = await crypto.subtle.importKey("raw", enc.encode(password), "PBKDF2", false, ["deriveBits"]);
  const bits = await crypto.subtle.deriveBits(
    // 10 000 iteracji (nie 100 000) — dopasowane do limitu 10ms CPU na darmowym planie Workers.
    // 100k iteracji = ~66ms, przekraczało limit i powodowało błędy 503 (Worker przerywany w trakcie działania)
    { name: "PBKDF2", salt: enc.encode(salt), iterations: 1e4, hash: "SHA-256" },
    keyMaterial,
    256
  );
  return Array.from(new Uint8Array(bits)).map((b) => b.toString(16).padStart(2, "0")).join("");
}
__name(hashPassword, "hashPassword");
__name2(hashPassword, "hashPassword");
function json(data, status = 200) {
  return new Response(JSON.stringify(data), {
    status,
    headers: { ...CORS, "Content-Type": "application/json", "Cache-Control": "no-store" }
  });
}
__name(json, "json");
__name2(json, "json");
export {
  worker_default as default
};
//# sourceMappingURL=worker.js.map

