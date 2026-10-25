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
    if (/^ask\./i.test(url.hostname)) {
      const m = url.pathname.match(/^\/(pl|sk|cs|cz|en)\/?$/i);
      if (url.pathname === "/" || m) {
        if (m) url.searchParams.set("lang", m[1].toLowerCase() === "cz" ? "cs" : m[1].toLowerCase());
        try {
          return await svfPage(env, url, ctx);
        } catch (e) {
          return new Response("B\u0142\u0105d: " + (e && e.message), { status: 500 });
        }
      }
      if (url.pathname === "/favicon.ico") return new Response(null, { status: 204 });
      if (!/^\/(ankieta(\/submit|\/tr)?|tydzien|r|raty)$/.test(url.pathname)) return new Response("Nie znaleziono", { status: 404, headers: { "Content-Type": "text/plain; charset=utf-8" } });
    }
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
      if (url.pathname === "/student-report" && request.method === "GET") {
        const who = await contactAuth(env, url.searchParams.get("ns_token"), url.searchParams.get("ns_email"), url.searchParams.get("ns_server"));
        if (!who) return json({ error: "Nieprawid\u0142owy lub wygas\u0142y token NS \u2014 zaloguj si\u0119 ponownie" }, 401);
        if (who.forbidden) return json({ error: "Tylko trener/admin" }, 403);
        await ensureTranscriptTables(env);
        try {
          await env.DB.prepare("CREATE TABLE IF NOT EXISTS student_report (ns_student_id TEXT, period TEXT, report TEXT, created_at INTEGER, PRIMARY KEY (ns_student_id, period))").run();
        } catch (e) {
        }
        const sid = String(url.searchParams.get("student_id") || "");
        const row = await env.DB.prepare("SELECT report FROM student_report WHERE ns_student_id = ? ORDER BY created_at DESC LIMIT 1").bind(sid).first();
        let rep = null;
        try {
          rep = row ? JSON.parse(row.report) : null;
        } catch (e) {
        }
        await srJobTable(env);
        const job = await env.DB.prepare("SELECT status, days, error, created_at, started_at, finished_at FROM student_report_job WHERE ns_student_id = ?").bind(sid).first();
        return json({ ok: true, report: rep, job: job || null });
      }
      if (url.pathname === "/student-report/run" && request.method === "POST") {
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
        await srJobTable(env);
        const days = Math.min(366, Math.max(7, Number(body.days) || 30)), now = Math.floor(Date.now() / 1e3);
        const range = body.range && /^\d{4}-\d{2}-\d{2}$/.test(String(body.range.from || "")) && /^\d{4}-\d{2}-\d{2}$/.test(String(body.range.to || "")) ? { from: body.range.from, to: body.range.to, label: String(body.range.label || "").slice(0, 80) } : null;
        const extra = JSON.stringify({ tasks: Array.isArray(body.tasks) ? body.tasks.slice(0, 60) : [], range }).slice(0, 6e4);
        await env.DB.prepare("INSERT INTO student_report_job (ns_student_id, days, status, error, requested_by, created_at, started_at, finished_at, tries, extra) VALUES (?, ?, 'queued', NULL, ?, ?, NULL, NULL, 0, ?) ON CONFLICT(ns_student_id) DO UPDATE SET days = excluded.days, status = 'queued', error = NULL, requested_by = excluded.requested_by, created_at = excluded.created_at, started_at = NULL, finished_at = NULL, tries = 0, extra = excluded.extra").bind(sid, days, String(who.email || ""), now, extra).run();
        return json({ ok: true, queued: true, job: { status: "queued", days, created_at: now } });
      }
      if (url.pathname === "/surveys/list" || url.pathname === "/surveys/link") {
        let body = {};
        if (request.method === "POST") {
          try {
            body = await request.json();
          } catch (e) {
          }
        }
        const src = request.method === "POST" ? body : { ns_token: url.searchParams.get("ns_token"), ns_email: url.searchParams.get("ns_email"), ns_server: url.searchParams.get("ns_server") };
        const who = await contactAuth(env, src.ns_token, src.ns_email, src.ns_server);
        if (!who) return json({ error: "Nieprawid\u0142owy lub wygas\u0142y token NS \u2014 zaloguj si\u0119 ponownie" }, 401);
        if (who.forbidden || !who.admin) return json({ error: "Tylko administrator" }, 403);
        if (url.pathname === "/surveys/link") {
          await svLinksTable(env);
          const key = String(body.row_key || "");
          if (!key) return json({ error: "Brak ankiety" }, 400);
          if (body.clear) await env.DB.prepare("DELETE FROM survey_links WHERE row_key = ?").bind(key).run();
          else await env.DB.prepare("INSERT INTO survey_links (row_key, ns_student_id, linked_by, linked_at) VALUES (?, ?, ?, ?) ON CONFLICT(row_key) DO UPDATE SET ns_student_id = excluded.ns_student_id, linked_by = excluded.linked_by, linked_at = excluded.linked_at").bind(key, body.student_id ? String(body.student_id) : "", String(who.email || ""), Math.floor(Date.now() / 1e3)).run();
          return json({ ok: true });
        }
        const r = await svList(env);
        return json(r, r.ok ? 200 : 400);
      }
      if (url.pathname === "/surveys/status" || url.pathname === "/surveys/bulk-mail" || url.pathname === "/surveys/export-sheet" || url.pathname === "/surveys/hide" || url.pathname === "/surveys/merge" || url.pathname === "/surveys/expand" || url.pathname === "/surveys/test-delete" || url.pathname === "/surveys/form-cfg") {
        let body = {};
        try {
          body = await request.json();
        } catch (e) {
        }
        if (request.method !== "POST") return json({ error: "POST" }, 405);
        const who = await contactAuth(env, body.ns_token, body.ns_email, body.ns_server);
        if (!who) return json({ error: "Nieprawid\u0142owy lub wygas\u0142y token NS \u2014 zaloguj si\u0119 ponownie" }, 401);
        if (who.forbidden || !who.admin) return json({ error: "Tylko administrator" }, 403);
        if (url.pathname === "/surveys/status") return json(await svSetStatus(env, who, body));
        if (url.pathname === "/surveys/form-cfg") {
          const cfg = await svfCfg(env);
          if (body.save) {
            const clean = /* @__PURE__ */ __name((u) => {
              u = String(u || "").trim().slice(0, 500);
              return /^https?:\/\/\S+$/.test(u) ? u : "";
            }, "clean");
            const L = body.links || {};
            cfg.links = {};
            Object.keys(SVF_LANGS).forEach((lg) => {
              const x = L[lg] || {};
              cfg.links[lg] = { film_kids: clean(x.film_kids), film_adults: clean(x.film_adults), esbs: clean(x.esbs), shop: clean(x.shop), book_title: String(x.book_title || "").trim().slice(0, 120), book_img: clean(x.book_img) };
            });
            if (body.live === true || body.live === false) await appSettingSet(env, "svf_live", body.live ? "1" : "0");
            const pb = String(body.public_base || "").trim().replace(/\/+$/, "");
            cfg.public_base = /^https:\/\/[a-z0-9.-]+$/i.test(pb) ? pb : "";
            if (Array.isArray(body.hide)) cfg.hide = body.hide.map((h) => String(h || "").slice(0, 300)).filter(Boolean).slice(0, 200);
            await appSettingSet(env, "svf_cfg", JSON.stringify(cfg));
            await appSettingSet(env, "svf_def3", "");
          }
          const D = await svfDef(env, !!body.save || !!body.fresh);
          const warm = {};
          if (Array.isArray(body.warm) && D.header) {
            for (const lg of body.warm) {
              if (SVF_LANGS[lg] && lg !== "pl") {
                const m = await svfTrAll(env, lg, svfStrings(D));
                warm[lg] = Object.keys(m).length;
              }
            }
          }
          const all = cfg.links || {}, flat = all.film_kids != null || all.film_adults != null || all.esbs != null;
          const links = flat ? { pl: { film_kids: all.film_kids || "", film_adults: all.film_adults || "", esbs: all.esbs || "", shop: "" } } : all;
          return json({ ok: true, live: await appSettingGet(env, "svf_live") !== "0", cfg, links, langs: SVF_LANGS, warm, strings: D.header ? svfStrings(D).length : 0, rest_all: D.rest_all || [], error: D.header ? null : D.error || null });
        }
        if (url.pathname === "/surveys/test-delete") {
          const key = String(body.row_key || "");
          if (!key) return json({ error: "Brak ankiety" }, 400);
          const id = await svSheetId(env);
          const meta = await svGoogle(env, encodeURIComponent(id) + "?fields=sheets.properties(sheetId,title)");
          if (!meta.ok) return json(meta, 400);
          const sh = (meta.data.sheets || []).find((x) => x.properties.title === SVF_TEST_TAB);
          if (!sh) return json({ error: "Brak zak\u0142adki testowej" }, 404);
          const v = await svGoogle(env, encodeURIComponent(id) + "/values/" + encodeURIComponent("'" + SVF_TEST_TAB + "'!A1:B2000"));
          if (!v.ok) return json(v, 400);
          const rows = v.data.values || [];
          let at = -1;
          for (let i = rows.length - 1; i >= 1; i--) {
            if (svKey(rows[i] || []) === key) {
              at = i;
              break;
            }
          }
          if (at < 0) return json({ error: "Nie znaleziono wiersza w zak\u0142adce testowej" }, 404);
          const d = await svGoogle(env, encodeURIComponent(id) + ":batchUpdate", { method: "POST", body: { requests: [{ deleteDimension: { range: { sheetId: sh.properties.sheetId, dimension: "ROWS", startIndex: at, endIndex: at + 1 } } }] } });
          if (!d.ok) return json(d, 400);
          return json({ ok: true });
        }
        if (url.pathname === "/surveys/expand") {
          const q = String(body.q || "").replace(/\s+/g, " ").trim().slice(0, 120);
          if (q.length < 2) return json({ error: "Wpisz has\u0142o" }, 400);
          let terms = [];
          try {
            const r2 = txParseJson(await txLlm(env, 'Pomagasz trenerowi p\u0142ynnej mowy (kurs dla os\xF3b, kt\xF3re si\u0119 j\u0105kaj\u0105) przeszukiwa\u0107 ankiety wst\u0119pne pisane po polsku, s\u0142owacku lub czesku. Dla has\u0142a trenera podaj 8\u201316 kr\xF3tkich s\u0142\xF3w lub zwrot\xF3w (1\u20133 s\u0142owa), kt\xF3rymi kursant albo rodzic m\xF3g\u0142 opisa\u0107 to samo w ankiecie: synonimy, potoczne okre\u015Blenia, typowe sytuacje, odmiany (rdzenie, np. \u201Etelefon\u201D, \u201Edzwoni\u201D), tak\u017Ce po s\u0142owacku/czesku, je\u015Bli to inne s\u0142owo. Nie u\u017Cywaj s\u0142owa \u201Eterapia\u201D. Odpowiedz WY\u0141\u0104CZNIE JSON-em: {"terms":["\u2026"]}', q, 600));
            terms = (r2 && Array.isArray(r2.terms) ? r2.terms : []).map((t) => String(t || "").replace(/\s+/g, " ").trim().slice(0, 40)).filter((t) => t.length >= 2).slice(0, 20);
          } catch (e) {
          }
          return json({ ok: true, q, terms });
        }
        if (url.pathname === "/surveys/merge") {
          await sstTable(env);
          await svLinksTable(env);
          const keys = (Array.isArray(body.row_keys) ? body.row_keys : [body.row_key]).map((k) => String(k || "").slice(0, 300)).filter(Boolean).slice(0, 20);
          const into = String(body.into_key || "").slice(0, 300);
          if (!keys.length) return json({ error: "Brak ankiety" }, 400);
          if (body.undo) {
            for (const k of keys) await env.DB.prepare("DELETE FROM survey_merge WHERE row_key = ?").bind(k).run();
            return json({ ok: true });
          }
          if (!into || keys.includes(into)) return json({ error: "Wybierz ankiet\u0119 g\u0142\xF3wn\u0105" }, 400);
          const main = await env.DB.prepare("SELECT ns_student_id FROM survey_links WHERE row_key = ?").bind(into).first();
          const now = Math.floor(Date.now() / 1e3);
          for (const k of keys) {
            await env.DB.prepare("INSERT OR REPLACE INTO survey_merge (row_key, into_key, merged_by, merged_at) VALUES (?, ?, ?, ?)").bind(k, into, String(who.email || ""), now).run();
            if (main && main.ns_student_id) await env.DB.prepare("INSERT INTO survey_links (row_key, ns_student_id, linked_by, linked_at) VALUES (?, ?, ?, ?) ON CONFLICT(row_key) DO UPDATE SET ns_student_id = excluded.ns_student_id, linked_by = excluded.linked_by, linked_at = excluded.linked_at").bind(k, main.ns_student_id, String(who.email || ""), now).run();
          }
          return json({ ok: true, count: keys.length });
        }
        if (url.pathname === "/surveys/hide") {
          await sstTable(env);
          const keys = (Array.isArray(body.row_keys) ? body.row_keys : [body.row_key]).map((k) => String(k || "").slice(0, 300)).filter(Boolean).slice(0, 200);
          if (!keys.length) return json({ error: "Brak ankiety" }, 400);
          for (const k of keys) {
            if (body.hidden === false) await env.DB.prepare("DELETE FROM survey_hidden WHERE row_key = ?").bind(k).run();
            else await env.DB.prepare("INSERT OR REPLACE INTO survey_hidden (row_key, hidden_by, hidden_at) VALUES (?, ?, ?)").bind(k, String(who.email || ""), Math.floor(Date.now() / 1e3)).run();
          }
          return json({ ok: true, count: keys.length });
        }
        if (url.pathname === "/surveys/bulk-mail") {
          body._base = url.origin;
          const r2 = await svBulkMail(env, who, body);
          return json(r2, r2.ok ? 200 : 400);
        }
        const r = await svExportSheet(env);
        return json(r, r.ok ? 200 : 400);
      }
      if (url.pathname === "/student-symptoms" || url.pathname === "/student-symptoms/save") {
        let body = {};
        if (request.method === "POST") {
          try {
            body = await request.json();
          } catch (e) {
          }
        }
        const src = request.method === "POST" ? body : { ns_token: url.searchParams.get("ns_token"), ns_email: url.searchParams.get("ns_email"), ns_server: url.searchParams.get("ns_server"), student_id: url.searchParams.get("student_id") };
        const who = await contactAuth(env, src.ns_token, src.ns_email, src.ns_server);
        if (!who) return json({ error: "Nieprawid\u0142owy lub wygas\u0142y token NS \u2014 zaloguj si\u0119 ponownie" }, 401);
        if (who.forbidden) return json({ error: "Tylko trener/admin" }, 403);
        const sid = String(src.student_id || "");
        if (!sid) return json({ error: "Brak student_id" }, 400);
        await ssTable(env);
        if (url.pathname === "/student-symptoms/save" && request.method === "POST" && !body.delete_check) {
          const lv = {};
          for (const [k, v] of Object.entries(body.levels || {})) {
            const n = Number(v);
            if (/^\d{1,3}$/.test(k) && n >= 0 && n <= 3) lv[k] = n;
          }
          const ex = (Array.isArray(body.extra) ? body.extra : []).slice(0, 40).map((x) => ({ name: String(x && x.name || "").slice(0, 120), level: Math.max(0, Math.min(3, Number(x && x.level) || 0)) })).filter((x) => x.name);
          await env.DB.prepare("INSERT INTO student_symptoms (ns_student_id, levels, extra, notes, updated_by, updated_at) VALUES (?, ?, ?, ?, ?, ?) ON CONFLICT(ns_student_id) DO UPDATE SET levels = excluded.levels, extra = excluded.extra, notes = excluded.notes, updated_by = excluded.updated_by, updated_at = excluded.updated_at").bind(sid, JSON.stringify(lv), JSON.stringify(ex), String(body.notes || "").slice(0, 8e3), String(who.email || ""), Math.floor(Date.now() / 1e3)).run();
          const d = /^\d{4}-\d{2}-\d{2}$/.test(String(body.date || "")) ? body.date : (/* @__PURE__ */ new Date()).toISOString().slice(0, 10);
          const kind = ["phone", "live", "online"].includes(body.kind) ? body.kind : "phone";
          await env.DB.prepare("CREATE TABLE IF NOT EXISTS symptom_checks (id INTEGER PRIMARY KEY AUTOINCREMENT, ns_student_id TEXT, date TEXT, kind TEXT, levels TEXT, extra TEXT, notes TEXT, created_by TEXT, created_at INTEGER)").run();
          const same = await env.DB.prepare("SELECT id FROM symptom_checks WHERE ns_student_id = ? AND date = ? AND kind = ?").bind(sid, d, kind).first();
          if (same) await env.DB.prepare("UPDATE symptom_checks SET levels = ?, extra = ?, notes = ?, created_by = ?, created_at = ? WHERE id = ?").bind(JSON.stringify(lv), JSON.stringify(ex), String(body.notes || "").slice(0, 8e3), String(who.email || ""), Math.floor(Date.now() / 1e3), same.id).run();
          else await env.DB.prepare("INSERT INTO symptom_checks (ns_student_id, date, kind, levels, extra, notes, created_by, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)").bind(sid, d, kind, JSON.stringify(lv), JSON.stringify(ex), String(body.notes || "").slice(0, 8e3), String(who.email || ""), Math.floor(Date.now() / 1e3)).run();
        }
        if (url.pathname === "/student-symptoms/save" && request.method === "POST" && body.delete_check) {
          await env.DB.prepare("DELETE FROM symptom_checks WHERE id = ? AND ns_student_id = ?").bind(Number(body.delete_check) || 0, sid).run();
        }
        const row = await env.DB.prepare("SELECT levels, extra, notes, updated_by, updated_at FROM student_symptoms WHERE ns_student_id = ?").bind(sid).first();
        let levels = {}, extra = [];
        try {
          levels = row && row.levels ? JSON.parse(row.levels) : {};
        } catch (e) {
        }
        try {
          extra = row && row.extra ? JSON.parse(row.extra) : [];
        } catch (e) {
        }
        let history = [];
        try {
          history = ((await env.DB.prepare("SELECT id, date, kind, levels, notes, created_by FROM symptom_checks WHERE ns_student_id = ? ORDER BY date, id").bind(sid).all()).results || []).map((h) => {
            let l = {};
            try {
              l = JSON.parse(h.levels || "{}");
            } catch (e) {
            }
            return { id: h.id, date: h.date, kind: h.kind, levels: l, notes: h.notes || "", by: h.created_by };
          });
        } catch (e) {
        }
        return json({ ok: true, levels, extra, notes: row && row.notes || "", updated_by: row && row.updated_by, updated_at: row && row.updated_at, history });
      }
      if (url.pathname === "/surveys/for-student" && request.method === "GET") {
        const who = await contactAuth(env, url.searchParams.get("ns_token"), url.searchParams.get("ns_email"), url.searchParams.get("ns_server"));
        if (!who) return json({ error: "Nieprawid\u0142owy lub wygas\u0142y token NS \u2014 zaloguj si\u0119 ponownie" }, 401);
        if (who.forbidden) return json({ error: "Tylko trener/admin" }, 403);
        const sid = String(url.searchParams.get("student_id") || "");
        if (!sid) return json({ error: "Brak student_id" }, 400);
        const r = await svForStudent(env, sid);
        try {
          r.edits = await seMerged(env, sid, r.row_key);
        } catch (e) {
          r.edits = null;
        }
        if (!r.ok) return json({ ok: true, row: null, header: [], edits: r.edits, sheet_error: r.error || null });
        return json(r);
      }
      if (url.pathname === "/admin/trainer-activity" && request.method === "GET") {
        const who = await contactAuth(env, url.searchParams.get("ns_token"), url.searchParams.get("ns_email"), url.searchParams.get("ns_server"));
        if (!who) return json({ error: "Nieprawid\u0142owy lub wygas\u0142y token NS \u2014 zaloguj si\u0119 ponownie" }, 401);
        if (who.forbidden || !who.admin) return json({ error: "Tylko admin" }, 403);
        return json(await trainerActivity(env));
      }
      if (url.pathname === "/surveys/edit" && request.method === "POST") {
        let body = {};
        try {
          body = await request.json();
        } catch (e) {
        }
        const who = await contactAuth(env, body.ns_token, body.ns_email, body.ns_server);
        if (!who) return json({ error: "Nieprawid\u0142owy lub wygas\u0142y token NS \u2014 zaloguj si\u0119 ponownie" }, 401);
        if (who.forbidden) return json({ error: "Tylko trener/admin" }, 403);
        const rowKey = String(body.row_key || "").slice(0, 300);
        const sid = body.student_id ? String(body.student_id) : rowKey ? "row:" + rowKey : "";
        if (!sid) return json({ error: "Brak student_id albo row_key" }, 400);
        await seTable(env);
        const answers = {};
        for (const [k, v] of Object.entries(body.answers || {})) if (/^\d{1,3}$/.test(k)) answers[k] = String(v == null ? "" : v).slice(0, 4e3);
        const added = (Array.isArray(body.added) ? body.added : []).slice(0, 100).map((x) => ({ group: String(x && x.group || "").slice(0, 60), q: String(x && x.q || "").slice(0, 300), a: String(x && x.a || "").slice(0, 4e3) })).filter((x) => x.q || x.a);
        await env.DB.prepare("INSERT INTO survey_edits (ns_student_id, answers, added, updated_by, updated_at) VALUES (?, ?, ?, ?, ?) ON CONFLICT(ns_student_id) DO UPDATE SET answers = excluded.answers, added = excluded.added, updated_by = excluded.updated_by, updated_at = excluded.updated_at").bind(sid, JSON.stringify(answers), JSON.stringify(added), String(who.email || ""), Math.floor(Date.now() / 1e3)).run();
        if (body.student_id && rowKey) await env.DB.prepare("DELETE FROM survey_edits WHERE ns_student_id = ?").bind("row:" + rowKey).run();
        return json({ ok: true, edits: await seGet(env, sid) });
      }
      if (url.pathname === "/surveys/edits" && request.method === "GET") {
        const who = await contactAuth(env, url.searchParams.get("ns_token"), url.searchParams.get("ns_email"), url.searchParams.get("ns_server"));
        if (!who) return json({ error: "Nieprawid\u0142owy lub wygas\u0142y token NS \u2014 zaloguj si\u0119 ponownie" }, 401);
        if (who.forbidden) return json({ error: "Tylko trener/admin" }, 403);
        const sidQ = url.searchParams.get("student_id"), rk = url.searchParams.get("row_key");
        if (!sidQ && !rk) return json({ error: "Brak student_id albo row_key" }, 400);
        return json({ ok: true, edits: await seMerged(env, sidQ, rk) });
      }
      if (url.pathname === "/surveys/check" || url.pathname === "/surveys/sheet") {
        let body = {};
        if (request.method === "POST") {
          try {
            body = await request.json();
          } catch (e) {
          }
        }
        const src = request.method === "POST" ? body : { ns_token: url.searchParams.get("ns_token"), ns_email: url.searchParams.get("ns_email"), ns_server: url.searchParams.get("ns_server") };
        const who = await contactAuth(env, src.ns_token, src.ns_email, src.ns_server);
        if (!who) return json({ error: "Nieprawid\u0142owy lub wygas\u0142y token NS \u2014 zaloguj si\u0119 ponownie" }, 401);
        if (who.forbidden || !who.admin) return json({ error: "Tylko administrator" }, 403);
        if (url.pathname === "/surveys/sheet" && request.method === "POST" && body.colmap_id) {
          await ensureGoogleTables(env);
          const x = (await svExtraSheets(env)).find((e) => e.id === String(body.colmap_id));
          if (!x) return json({ error: "Nie ma takiego arkusza" }, 404);
          const main = await svRowsOne(env, await svSheetId(env)), R = await svRowsOne(env, x.id);
          if (!main.ok) return json(main, 400);
          if (!R.ok) return json(R, 400);
          if (body.recompute) await appSettingSet(env, "svmap_" + x.id, "null");
          if (body.map && typeof body.map === "object") {
            const map = {}, used = /* @__PURE__ */ new Set();
            for (const [k, v] of Object.entries(body.map)) {
              const j = +k, p = v === "" || v == null ? null : +v;
              if (j >= 0 && j < R.header.length && p != null && p >= 0 && p < main.header.length && !used.has(p)) {
                map[j] = p;
                used.add(p);
              }
            }
            await appSettingSet(env, "svmap_" + x.id, JSON.stringify({ hash: await svHeaderHash(R.header), map, manual: true, at: Math.floor(Date.now() / 1e3), by: who.email }));
          }
          const M = await svColMap(env, x, R.header, main.header);
          const sample = R.rows.slice(-1)[0] || [];
          return json({ ok: true, label: x.label, lang: x.lang, sk_header: R.header, pl_header: main.header, map: M.map, manual: !!M.manual, sample });
        }
        if (url.pathname === "/surveys/sheet" && request.method === "POST" && (body.extra || body.remove_extra)) {
          await ensureGoogleTables(env);
          let list = await svExtraSheets(env);
          if (body.remove_extra) list = list.filter((x) => x.id !== String(body.remove_extra));
          else {
            const m = String(body.sheet || "").match(/[-\w]{30,}/);
            if (!m) return json({ error: "Wklej link do arkusza Google" }, 400);
            if (m[0] === await svSheetId(env)) return json({ error: "To jest ju\u017C arkusz g\u0142\xF3wny" }, 400);
            list = list.filter((x) => x.id !== m[0]).concat([{ id: m[0], label: String(body.label || "SK").slice(0, 20), lang: /^(sk|cs|pl|en)$/.test(String(body.lang || "")) ? body.lang : "sk" }]);
          }
          await appSettingSet(env, "survey_sheets_extra", JSON.stringify(list));
          return json(await svCheck(env));
        }
        if (url.pathname === "/surveys/sheet" && request.method === "POST") {
          const m = String(body.sheet || "").match(/[-\w]{30,}/);
          if (!m) return json({ error: "Wklej link do arkusza Google" }, 400);
          await ensureGoogleTables(env);
          await appSettingSet(env, "survey_sheet_id", m[0]);
        }
        return json(await svCheck(env));
      }
      if (url.pathname === "/offers/send" && request.method === "POST") return await handleOfferSend(request, env);
      if (url.pathname === "/offers/translate" && request.method === "POST") return await handleOfferTranslate(request, env);
      if (url.pathname === "/raty" && request.method === "GET") return ofRatyPage(url);
      if (url.pathname === "/ankieta" && request.method === "GET") return await svfPage(env, url, ctx);
      if (url.pathname === "/ankieta/submit" && request.method === "POST") return await svfSubmit(request, env, ctx);
      if (url.pathname === "/ankieta/tr" && request.method === "GET") {
        const lg = url.searchParams.get("lang");
        const D = await svfDef(env, false, ctx);
        if (!D.header) return json({ error: D.error || "" }, 500);
        return json({ ok: true, tr: await svfTrAll(env, lg, svfStrings(D)) });
      }
      if (url.pathname === "/tydzien" && request.method === "GET") return await pwPage(request, env, url);
      if (url.pathname === "/tydzien" && request.method === "POST") return await pwSubmit(request, env);
      if (url.pathname === "/parent-weekly" || url.pathname === "/parent-weekly/sub") return await pwPanel(request, env, url);
      if (url.pathname === "/r" && (request.method === "GET" || request.method === "POST")) return await svReplyPage(request, env, url);
      if (url.pathname === "/offers/sent-file" && request.method === "GET") {
        const who = await contactAuth(env, url.searchParams.get("ns_token"), url.searchParams.get("ns_email"), url.searchParams.get("ns_server"));
        if (!who) return json({ error: "Nieprawid\u0142owy lub wygas\u0142y token NS \u2014 zaloguj si\u0119 ponownie" }, 401);
        if (who.forbidden || !who.admin) return json({ error: "Tylko administrator" }, 403);
        await sstTable(env);
        const r = await env.DB.prepare("SELECT html_key, pdf_key, pdf_name FROM offer_meta WHERE id = ?").bind(Number(url.searchParams.get("id")) || 0).first();
        const k = r && (url.searchParams.get("part") === "pdf" ? r.pdf_key : r.html_key);
        if (!k) return json({ error: "Brak kopii tej wiadomo\u015Bci" }, 404);
        const o = await env.RECORDINGS.get(k);
        if (!o) return json({ error: "Plik nie istnieje" }, 404);
        return new Response(o.body, { headers: { "Content-Type": o.httpMetadata && o.httpMetadata.contentType || "application/octet-stream", "Access-Control-Allow-Origin": "*", "Cache-Control": "private, max-age=300" } });
      }
      if (url.pathname.startsWith("/offers/reminders")) {
        let body = {};
        if (request.method === "POST") {
          try {
            body = await request.json();
          } catch (e) {
          }
        }
        const src = request.method === "POST" ? body : { ns_token: url.searchParams.get("ns_token"), ns_email: url.searchParams.get("ns_email"), ns_server: url.searchParams.get("ns_server") };
        const who = await contactAuth(env, src.ns_token, src.ns_email, src.ns_server);
        if (!who) return json({ error: "Nieprawid\u0142owy lub wygas\u0142y token NS \u2014 zaloguj si\u0119 ponownie" }, 401);
        if (who.forbidden || !who.admin) return json({ error: "Tylko administrator" }, 403);
        await ofTables(env);
        if (url.pathname === "/offers/reminders/deposit" && request.method === "POST") {
          const on = body.on !== false;
          let id = Number(body.id) || 0;
          if (!id && on) {
            const to = String(body.to_email || "").trim(), start = String(body.start || "");
            if (!/@/.test(to) || !/^\d{4}-\d{2}-\d{2}$/.test(start)) return json({ error: "Podaj e-mail i dat\u0119 startu kursu" }, 400);
            const r0 = await env.DB.prepare("INSERT INTO course_reminders (to_email, student_name, start_date, child, created_by, created_at) VALUES (?, ?, ?, ?, ?, ?)").bind(to, String(body.name || "").slice(0, 120), start, body.child ? 1 : 0, who.email, Math.floor(Date.now() / 1e3)).run();
            id = r0.meta && r0.meta.last_row_id;
          }
          await env.DB.prepare("UPDATE course_reminders SET deposit_at = ?, deposit_by = ? WHERE id = ?").bind(on ? Math.floor(Date.now() / 1e3) : null, on ? String(who.email || "") : null, id).run();
          const r = await env.DB.prepare("SELECT * FROM course_reminders WHERE id = ?").bind(id).first();
          return json({ ok: true, id, send_on: r ? ofAddDays(r.start_date, -7) : null });
        }
        if (url.pathname === "/offers/reminders/remind" && request.method === "POST") {
          const on = body.on !== false, now = Math.floor(Date.now() / 1e3);
          let id = Number(body.id) || 0;
          if (!id && on) {
            const to = String(body.to_email || "").trim(), start = String(body.start || "");
            if (!/@/.test(to) || !/^\d{4}-\d{2}-\d{2}$/.test(start)) return json({ error: "Podaj e-mail i dat\u0119 startu kursu" }, 400);
            const r2 = await env.DB.prepare("INSERT INTO course_reminders (to_email, student_name, start_date, child, created_by, created_at) VALUES (?, ?, ?, ?, ?, ?)").bind(to, String(body.name || "").slice(0, 120), start, body.child ? 1 : 0, who.email, now).run();
            id = r2.meta && r2.meta.last_row_id;
          }
          await env.DB.prepare("UPDATE course_reminders SET remind_at = ?, remind_by = ? WHERE id = ?").bind(on ? now : null, on ? String(who.email || "") : null, id).run();
          const r = await env.DB.prepare("SELECT * FROM course_reminders WHERE id = ?").bind(id).first();
          return json({ ok: true, id, send_on: r ? ofAddDays(r.start_date, -7) : null, sent_at: r && r.sent_at });
        }
        if (url.pathname === "/offers/reminders/send-now" && request.method === "POST") {
          const r = await env.DB.prepare("SELECT * FROM course_reminders WHERE id = ? AND COALESCE(cancelled, 0) = 0").bind(Number(body.id) || 0).first();
          if (!r) return json({ error: "Nie ma takiej rezerwacji" }, 404);
          const res = await ofSendCourseInfo(env, r);
          return json(res.ok ? { ok: true } : { error: res.error }, res.ok ? 200 : 502);
        }
        if (url.pathname === "/offers/reminders/preview" && request.method === "GET") {
          return json({ ok: true, html: ofCourseInfoHtml(String(url.searchParams.get("start") || "2026-01-01")) });
        }
        if (url.pathname === "/offers/reminders/cancel" && request.method === "POST") {
          await env.DB.prepare("UPDATE course_reminders SET cancelled = 1 WHERE id = ?").bind(Number(body.id) || 0).run();
          return json({ ok: true });
        }
        const rs = await env.DB.prepare("SELECT id, to_email, student_name, start_date, created_by, created_at, sent_at, error, deposit_at, deposit_by FROM course_reminders WHERE COALESCE(cancelled, 0) = 0 ORDER BY start_date DESC LIMIT 100").all();
        try {
          const ra = (await env.DB.prepare("SELECT id, remind_at FROM course_reminders WHERE remind_at IS NOT NULL").all()).results || [];
          const m = {};
          ra.forEach((x) => m[x.id] = x.remind_at);
          (rs.results || []).forEach((x) => {
            x.remind_at = m[x.id] || null;
          });
        } catch (e) {
        }
        try {
          const ems = [...new Set((rs.results || []).map((x) => String(x.to_email || "").toLowerCase()).filter(Boolean))];
          if (ems.length) {
            const pg = new Client({ connectionString: env.HYPERDRIVE.connectionString });
            try {
              await pg.connect();
              const has = new Set((await pg.query("SELECT LOWER(email) AS e FROM users WHERE LOWER(email) = ANY($1) AND deleted_at IS NULL", [ems])).rows.map((r) => r.e));
              (rs.results || []).forEach((x) => {
                x.has_account = has.has(String(x.to_email || "").toLowerCase());
              });
            } finally {
              try {
                await pg.end();
              } catch (e) {
              }
            }
          }
        } catch (e) {
        }
        return json({ ok: true, reminders: rs.results || [], to: ofReminderTo(env) });
      }
      if (url.pathname.startsWith("/research/notes")) {
        let body = {};
        if (request.method === "POST") {
          try {
            body = await request.json();
          } catch (e) {
          }
        }
        const src = request.method === "POST" ? body : { ns_token: url.searchParams.get("ns_token"), ns_email: url.searchParams.get("ns_email"), ns_server: url.searchParams.get("ns_server") };
        const who = await contactAuth(env, src.ns_token, src.ns_email, src.ns_server);
        if (!who) return json({ error: "Nieprawid\u0142owy lub wygas\u0142y token NS \u2014 zaloguj si\u0119 ponownie" }, 401);
        if (who.forbidden || !who.admin) return json({ error: "Tylko administrator" }, 403);
        if (url.pathname === "/research/notes" && request.method === "GET") return json(await rsNotesList(env));
        if (url.pathname === "/research/notes/save" && request.method === "POST") {
          const r = await rsNotesSave(env, who, body);
          return json(r, r.ok ? 200 : 400);
        }
        if (url.pathname === "/research/notes/delete" && request.method === "POST") {
          await rsNotesTable(env);
          await env.DB.prepare("UPDATE research_notes SET deleted = 1, updated_by = ?, updated_at = ? WHERE slug = ?").bind(String(who.email || ""), Math.floor(Date.now() / 1e3), String(body.slug || "")).run();
          return json({ ok: true });
        }
      }
      if ((url.pathname === "/research/inventory" || url.pathname === "/research/export") && request.method === "GET") {
        const who = await contactAuth(env, url.searchParams.get("ns_token"), url.searchParams.get("ns_email"), url.searchParams.get("ns_server"));
        if (!who) return json({ error: "Nieprawid\u0142owy lub wygas\u0142y token NS \u2014 zaloguj si\u0119 ponownie" }, 401);
        if (who.forbidden || !who.admin) return json({ error: "Tylko administrator" }, 403);
        if (url.pathname === "/research/inventory") return json(await rsInventory(env));
        const ex = await rsExport(env, url);
        return new Response(ex.csv, { headers: { "Content-Type": "text/csv; charset=utf-8", "X-Rows": String(ex.n), "Access-Control-Allow-Origin": "*", "Access-Control-Expose-Headers": "X-Rows" } });
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
      if (url.pathname === "/geo" && request.method === "GET") {
        const cf = request.cf || {};
        const lat = Number(cf.latitude), lon = Number(cf.longitude);
        if (!isFinite(lat) || !isFinite(lon)) return json({ ok: false, error: "Brak lokalizacji" });
        return json({ ok: true, lat, lon, city: cf.city || null, country: cf.country || null, source: "ip" });
      }
      if (url.pathname === "/speech-rate" && request.method === "GET") {
        const who = await contactAuth(env, url.searchParams.get("ns_token"), url.searchParams.get("ns_email"), url.searchParams.get("ns_server"));
        if (!who) return json({ error: "Nieprawid\u0142owy lub wygas\u0142y token NS \u2014 zaloguj si\u0119 ponownie" }, 401);
        if (who.forbidden) return json({ error: "Tylko trener/admin" }, 403);
        const sid = String(url.searchParams.get("student_id") || "");
        if (!sid) return json({ error: "Brak student_id" }, 400);
        return json(await txSpeechRate(env, sid));
      }
      if (url.pathname === "/speech-rate/record" && request.method === "GET") {
        const me = await coachStudentFromRequest(request, env, url, null);
        if (!me) return json({ error: "Nieprawid\u0142owy lub wygas\u0142y token NS \u2014 zaloguj si\u0119 ponownie" }, 401);
        const rid = String(url.searchParams.get("record_id") || "");
        if (!rid || !me.id) return json({ error: "Brak record_id" }, 400);
        return json(await txSpeechRateRecord(env, me.id, rid));
      }
      if (url.pathname === "/speech-rate/mine" && request.method === "GET") {
        const me = await coachStudentFromRequest(request, env, url, null);
        if (!me) return json({ error: "Nieprawid\u0142owy lub wygas\u0142y token NS \u2014 zaloguj si\u0119 ponownie" }, 401);
        if (!me.id) return json({ ok: true, items: [], weeks: [], d7: { n: 0 }, prev7: { n: 0 }, d30: { n: 0 }, all: { n: 0 } });
        return json(await txSpeechRate(env, me.id));
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
      if (url.pathname === "/transcripts/phone-pending" && request.method === "POST") {
        let body = {};
        try {
          body = await request.json();
        } catch (e) {
        }
        const me = await coachStudentFromRequest(request, env, null, body);
        if (!me || !me.id) return json({ error: "Nieprawid\u0142owy lub wygas\u0142y token NS \u2014 zaloguj si\u0119 ponownie" }, 401);
        return json(await txPhonePending(env, me.id, body));
      }
      if (url.pathname === "/transcripts/phone" && request.method === "POST") {
        let body = {};
        try {
          body = await request.json();
        } catch (e) {
        }
        const me = await coachStudentFromRequest(request, env, null, body);
        if (!me || !me.id) return json({ error: "Nieprawid\u0142owy lub wygas\u0142y token NS \u2014 zaloguj si\u0119 ponownie" }, 401);
        let any = false;
        if (body.translator) {
          const ta = await translatorAuth(env, body.ns_token, body.ns_email, body.ns_server || "new");
          any = !!(ta && ta.ok);
          if (!any) return json({ error: "Tylko trener/admin" }, 403);
        }
        return json(await txPhoneTranscript(env, me.id, body, any));
      }
      if (url.pathname === "/transcripts/phone-queue" && request.method === "GET") {
        const ta = await translatorAuth(env, url.searchParams.get("ns_token"), url.searchParams.get("ns_email"), url.searchParams.get("ns_server"));
        if (!ta) return json({ error: "Nieprawid\u0142owy lub wygas\u0142y token NS \u2014 zaloguj si\u0119 ponownie" }, 401);
        if (!ta.ok) return json({ error: "Tylko trener/admin" }, 403);
        return json(await txPhoneQueue(env, Number(url.searchParams.get("limit")) || 3, url.searchParams.get("scope") === "all"));
      }
      if (url.pathname === "/transcripts/phone-release" && request.method === "POST") {
        let body = {};
        try {
          body = await request.json();
        } catch (e) {
        }
        const ta = await translatorAuth(env, body.ns_token, body.ns_email, body.ns_server || "new");
        if (!ta) return json({ error: "Nieprawid\u0142owy lub wygas\u0142y token NS \u2014 zaloguj si\u0119 ponownie" }, 401);
        if (!ta.ok) return json({ error: "Tylko trener/admin" }, 403);
        try {
          await env.DB.prepare("DELETE FROM phone_pending WHERE ns_record_id = ?").bind(String(body.record_id || "")).run();
        } catch (e) {
        }
        return json({ ok: true });
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
    ctx.waitUntil(srJobTick(env).catch(() => {
    }));
    ctx.waitUntil(ofReminderTick(env).catch(() => {
    }));
    ctx.waitUntil(pwTick(env).catch(() => {
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
      const addDays = /* @__PURE__ */ __name((ymd, n) => {
        const d = /* @__PURE__ */ new Date(ymd + "T12:00:00Z");
        d.setUTCDate(d.getUTCDate() + n);
        return d.toISOString().slice(0, 10);
      }, "addDays");
      const periods = v.rows.map((row) => {
        const visitDay = warsawDay(row.date), to = prevDay(visitDay);
        const gm = String(row.additional_requirements || "").match(/⏰\s*Dogrywka:\s*(\d+)/);
        const p = { row, from, to, grace: gm ? Math.min(20, Number(gm[1])) : 3, per: {} };
        reqs.filter((r) => r.visit_date_id === row.id).forEach((r) => {
          p.per[r.record_category_id] = { category_id: r.record_category_id, category_name: r.name_pl || catNames[r.record_category_id] || "", required_records: Number(r.records_count) || 0, all_records: 0, correct_records: 0, late_records: 0 };
        });
        from = visitDay;
        return p;
      });
      const pOf = /* @__PURE__ */ __name((day) => periods.findIndex((p) => day >= p.from && day <= p.to), "pOf");
      const cell = /* @__PURE__ */ __name((p, cat) => p.per[cat] || (p.per[cat] = { category_id: cat, category_name: catNames[cat] || "", required_records: 0, all_records: 0, correct_records: 0, late_records: 0 }), "cell");
      const owner = recDays.map((r) => r.day ? pOf(r.day) : -1);
      recDays.forEach((r, i) => {
        if (owner[i] < 0) return;
        const c = cell(periods[owner[i]], r.cat);
        c.all_records++;
        if (r.ok) c.correct_records++;
      });
      const order = recDays.map((r, i) => i).filter((i) => recDays[i].day && recDays[i].ok).sort((a, b) => recDays[a].day.localeCompare(recDays[b].day));
      periods.forEach((p, pi) => {
        if (!p.grace) return;
        const end = periods[pi + 1] ? periods[pi + 1].to : "9999-12-31";
        let taken = 0;
        for (const i of order) {
          if (taken >= p.grace) break;
          const r = recDays[i];
          if (r.day <= p.to || r.day > end) continue;
          if (owner[i] !== -1 && owner[i] <= pi) continue;
          const c = p.per[r.cat];
          if (!c || !(c.required_records > 0) || c.correct_records >= c.required_records) continue;
          if (owner[i] >= 0) {
            const q = cell(periods[owner[i]], r.cat);
            q.all_records--;
            q.correct_records--;
          }
          c.all_records++;
          c.correct_records++;
          c.late_records++;
          owner[i] = pi;
          taken++;
        }
      });
      stats = periods.map((p) => {
        const cats = Object.values(p.per).filter((c) => c.required_records > 0 || c.all_records > 0);
        const dia = diaryDays.filter((d) => d.day && d.day >= p.from && d.day <= p.to);
        return {
          visit_id: p.row.id,
          start_range: p.from,
          end_range: p.to,
          recorded_records_categories: cats,
          grace_days: p.grace,
          late_records: cats.reduce((a, c) => a + (c.late_records || 0), 0),
          all_records: cats.reduce((a, c) => a + c.all_records, 0),
          correct_records: cats.reduce((a, c) => a + c.correct_records, 0),
          required_records: cats.reduce((a, c) => a + c.required_records, 0),
          all_diaries: dia.length,
          correct_diaries: dia.filter((d) => d.ok).length,
          required_diaries: Number(p.row.required_diaries_count) || 0
        };
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
var GOOGLE_OAUTH_SCOPE = "https://www.googleapis.com/auth/calendar https://www.googleapis.com/auth/gmail.send https://www.googleapis.com/auth/spreadsheets";
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
    gmail_ok: /gmail\.send/.test(await appSettingGet(env, "google_scopes") || ""),
    sheets_ok: /spreadsheets/.test(await appSettingGet(env, "google_scopes") || "")
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
async function txPrioGet(env) {
  try {
    const r = await env.DB.prepare("SELECT v FROM tx_kv WHERE k = 'prio_sids'").first();
    const o = r && r.v ? JSON.parse(r.v) : {};
    const now = Date.now();
    for (const k of Object.keys(o)) if (now - o[k] > 2 * 864e5) delete o[k];
    return o;
  } catch (e) {
    return {};
  }
}
__name(txPrioGet, "txPrioGet");
__name2(txPrioGet, "txPrioGet");
async function txPrioAdd(env, sid) {
  const o = await txPrioGet(env);
  if (o[String(sid)] && Date.now() - o[String(sid)] < 36e5) return;
  o[String(sid)] = Date.now();
  const keys = Object.keys(o).sort((a, b) => o[b] - o[a]).slice(0, 10);
  const n = {};
  for (const k of keys) n[k] = o[k];
  await env.DB.prepare("INSERT INTO tx_kv (k, v) VALUES ('prio_sids', ?) ON CONFLICT(k) DO UPDATE SET v = excluded.v").bind(JSON.stringify(n)).run();
}
__name(txPrioAdd, "txPrioAdd");
__name2(txPrioAdd, "txPrioAdd");
async function txTalkStats(env, sid) {
  await ensureTranscriptTables(env);
  const pg = new Client({ connectionString: env.HYPERDRIVE.connectionString });
  await pg.connect();
  let recs = [], first = "";
  try {
    recs = (await pg.query(`SELECT r.id::text AS id, COALESCE(r.date, r.created_at::date) AS d, r.review_details, COALESCE(rct.name, rc.name) AS category
      FROM records r LEFT JOIN record_categories rc ON rc.id = r.record_category_id
      LEFT JOIN record_category_translations rct ON rct.record_category_id = rc.id AND rct.locale = 'pl'
      WHERE r.user_id::text = $1 AND r.deleted_at IS NULL AND r.record_file IS NOT NULL ORDER BY r.created_at DESC LIMIT 3000`, [String(sid)])).rows;
    try {
      const u = (await pg.query("SELECT first_name FROM users WHERE id::text = $1", [String(sid)])).rows[0];
      first = String(u && u.first_name || "").trim().toLowerCase();
    } catch (e) {
    }
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
    const rs = await env.DB.prepare(`SELECT ns_record_id, dur, words, speakers, spk_src, spk_names, spk_people FROM record_transcript WHERE dur > 0 AND ns_record_id IN (${chunk.map(() => "?").join(",")})`).bind(...chunk).all();
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
  const J = /* @__PURE__ */ __name((x, def) => {
    try {
      return x ? JSON.parse(x) : def;
    } catch (e) {
      return def;
    }
  }, "J");
  for (const r of recs) {
    const t = tx[r.id];
    const d = ymd(r.d);
    const has = t ? 1 : 0;
    if (!t) noText++;
    const min = t ? (Number(t.dur) || 0) / 60 : 0;
    let words = 0;
    if (t) {
      const wc = J(t.words, []), sp = J(t.speakers, null), names = J(t.spk_names, {}) || {}, people = J(t.spk_people, {}) || {};
      const spOk = sp && sp.length === wc.length;
      const persons = spOk ? new Set(sp).size : 1;
      if (persons < 2) {
        if (txIsMonolog(r.category) || t.spk_src === "ai-one") words = wc.length;
      } else {
        let idx = null;
        for (const k of Object.keys(people)) if (String(people[k]) === String(sid)) idx = Number(k);
        if (idx === null) for (const k of Object.keys(names)) {
          const nm = String(names[k] || "").toLowerCase();
          if (/kursant|kurzist|student/.test(nm) || first && nm.indexOf(first) >= 0) {
            idx = Number(k);
            break;
          }
        }
        if (idx === null && t.spk_src === "ai-text") idx = 0;
        if (idx !== null) words = sp.filter((x) => x === idx).length;
      }
    }
    const c = cats[r.category || "(bez kategorii)"] = cats[r.category || "(bez kategorii)"] || { category: r.category || "(bez kategorii)", n: 0, min: 0, words: 0, n30: 0, min30: 0, words30: 0, n7: 0, min7: 0, words7: 0, t: 0, t30: 0, t7: 0 };
    c.n++;
    c.t += has;
    c.min += min;
    c.words += words;
    if (d >= d30) {
      c.n30++;
      c.t30 += has;
      c.min30 += min;
      c.words30 += words;
    }
    if (d >= d7) {
      c.n7++;
      c.t7 += has;
      c.min7 += min;
      c.words7 += words;
    }
    const add = /* @__PURE__ */ __name((o) => {
      o.n++;
      o.t = (o.t || 0) + has;
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
  const fix = /* @__PURE__ */ __name((o) => ({ n: o.n, with_text: o.t || 0, min: r1(o.min), words: o.words }), "fix");
  if (noText) {
    try {
      await txPrioAdd(env, sid);
    } catch (e) {
    }
  }
  return {
    ok: true,
    totals: { all: fix(tot.all), d30: fix(tot.d30), d7: fix(tot.d7), today: fix(tot.today), prev7: fix(tot.prev7), prev30: fix(tot.prev30) },
    categories: Object.values(cats).map((c) => ({ category: c.category, n: c.n, t: c.t, min: r1(c.min), words: c.words, n30: c.n30, t30: c.t30, min30: r1(c.min30), words30: c.words30, n7: c.n7, t7: c.t7, min7: r1(c.min7), words7: c.words7 })).sort((a, b) => b.min - a.min),
    recordings_total: recs.length,
    weeks: (() => {
      const cur = weekKey(today), out = [];
      for (let i = 11; i >= 0; i--) {
        const d = /* @__PURE__ */ new Date(cur + "T12:00:00Z");
        d.setUTCDate(d.getUTCDate() - 7 * i);
        const k = d.toISOString().slice(0, 10);
        const w = weeks[k] || { week: k, min: 0, words: 0, n: 0 };
        out.push({ week: k, n: w.n, with_text: w.t || 0, min: r1(w.min), words: w.words });
      }
      const first2 = out.findIndex((w) => w.n > 0);
      return first2 > 0 ? out.slice(first2) : out;
    })(),
    today,
    without_text: noText,
    note: "Nagrania: wszystkie. Minuty i s\u0142owa: tylko nagrania z tekstem (reszta przepisywana w tle). S\u0142owa: monologi z jednym g\u0142osem + s\u0142owa kursanta w rozmowach podzielonych na osoby."
  };
}
__name(txTalkStats, "txTalkStats");
__name2(txTalkStats, "txTalkStats");
var SR_VOW = "a\u0105e\u0119io\xF3uy\xE1\xE9\u011B\xED\xFA\u016F\xFD\xE4\xF4";
function srSyl(w) {
  const x = String(w || "").toLowerCase().replace(/[^\p{L}]/gu, "").replace(/(\p{L})\1+/gu, "$1");
  let n = 0;
  for (let i = 0; i < x.length; i++) {
    const c = x[i];
    if (SR_VOW.indexOf(c) < 0) continue;
    if (c === "i" && i > 0 && SR_VOW.indexOf(x[i - 1]) < 0 && i + 1 < x.length && SR_VOW.indexOf(x[i + 1]) >= 0) continue;
    n++;
  }
  return n;
}
__name(srSyl, "srSyl");
__name2(srSyl, "srSyl");
function srMeasure(words, dur, minDur) {
  const W = [];
  for (const w of words || []) {
    if (Array.isArray(w)) W.push([String(w[0] || ""), Number(w[1]), Number(w[2])]);
    else if (w && typeof w === "object") W.push([String(w.word || w.w || w.text || ""), Number(w.start != null ? w.start : w.s), Number(w.end != null ? w.end : w.e)]);
  }
  const ok = W.filter((w) => isFinite(w[1]) && isFinite(w[2]) && w[2] >= w[1]);
  if (ok.length < 10) return null;
  let syl = 0, speech = 0, nPause = 0;
  for (let i = 0; i < ok.length; i++) {
    syl += srSyl(ok[i][0]);
    speech += ok[i][2] - ok[i][1];
    if (i > 0) {
      const g = ok[i][1] - ok[i - 1][2];
      if (g > 0 && g < 0.3) speech += g;
      else if (g >= 1) nPause++;
    }
  }
  const total = Math.max(Number(dur) || 0, ok[ok.length - 1][2]);
  if (total < (minDur == null ? 60 : minDur) || speech < 5 || !syl) return null;
  const r1 = /* @__PURE__ */ __name((v) => Math.round(v * 10) / 10, "r1");
  return {
    dur: r1(total),
    words: ok.length,
    syl,
    speech_sec: r1(speech),
    pause_sec: r1(Math.max(0, total - speech)),
    pause_pct: Math.round(Math.max(0, total - speech) / total * 100),
    pauses_1s: nPause,
    syl_min: Math.round(syl / total * 60),
    syl_min_speech: Math.round(syl / speech * 60),
    wpm: Math.round(ok.length / total * 60),
    wpm_speech: Math.round(ok.length / speech * 60)
  };
}
__name(srMeasure, "srMeasure");
__name2(srMeasure, "srMeasure");
async function txSpeechRate(env, sid) {
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
  recs = recs.filter((r) => txIsMonolog(r.category) && !isNotCountedReview(r.review_details));
  const tx = {};
  for (let i = 0; i < recs.length; i += 90) {
    const chunk = recs.slice(i, i + 90).map((r) => r.id);
    if (!chunk.length) break;
    const rs = await env.DB.prepare(`SELECT ns_record_id, dur, words, speakers FROM record_transcript WHERE dur > 0 AND ns_record_id IN (${chunk.map(() => "?").join(",")})`).bind(...chunk).all();
    for (const x of rs.results || []) tx[x.ns_record_id] = x;
  }
  const ymd = /* @__PURE__ */ __name((d) => (d instanceof Date ? d.toISOString() : String(d || "")).slice(0, 10), "ymd");
  const weekKey = /* @__PURE__ */ __name((s) => {
    const d = /* @__PURE__ */ new Date(s + "T12:00:00Z");
    d.setUTCDate(d.getUTCDate() - (d.getUTCDay() + 6) % 7);
    return d.toISOString().slice(0, 10);
  }, "weekKey");
  const items = [];
  let noText = 0;
  for (const r of recs) {
    const t = tx[r.id];
    if (!t) {
      noText++;
      continue;
    }
    let words = [], sp = null;
    try {
      words = JSON.parse(t.words || "[]");
    } catch (e) {
    }
    try {
      sp = t.speakers ? JSON.parse(t.speakers) : null;
    } catch (e) {
    }
    if (sp && sp.length === words.length && new Set(sp).size > 1) continue;
    const m = srMeasure(words, t.dur);
    if (!m) continue;
    items.push(Object.assign({ record_id: r.id, date: ymd(r.d), category: r.category || "" }, m));
  }
  items.sort((a, b) => b.date.localeCompare(a.date));
  const med = /* @__PURE__ */ __name((arr) => {
    if (!arr.length) return null;
    const a = arr.slice().sort((x, y) => x - y), k = a.length >> 1;
    return a.length % 2 ? a[k] : Math.round((a[k - 1] + a[k]) / 2);
  }, "med");
  const sum = /* @__PURE__ */ __name((l) => l.length ? { n: l.length, syl_min: med(l.map((x) => x.syl_min)), syl_min_speech: med(l.map((x) => x.syl_min_speech)), wpm: med(l.map((x) => x.wpm)), wpm_speech: med(l.map((x) => x.wpm_speech)), pause_pct: med(l.map((x) => x.pause_pct)) } : { n: 0 }, "sum");
  const now = Date.now(), dd = /* @__PURE__ */ __name((n) => new Date(now - n * 864e5).toISOString().slice(0, 10), "dd");
  const weeks = {};
  for (const x of items) {
    const k = weekKey(x.date);
    (weeks[k] = weeks[k] || []).push(x);
  }
  if (noText) {
    try {
      await txPrioAdd(env, sid);
    } catch (e) {
    }
  }
  return {
    ok: true,
    d7: sum(items.filter((x) => x.date >= dd(6))),
    prev7: sum(items.filter((x) => x.date >= dd(13) && x.date < dd(6))),
    d30: sum(items.filter((x) => x.date >= dd(29))),
    all: sum(items),
    weeks: Object.keys(weeks).sort().slice(-12).map((k) => Object.assign({ week: k }, sum(weeks[k]))),
    items: items.slice(0, 30),
    without_text: noText,
    note: "Tylko monologi/czytanie od 1 minuty. Sylaby i s\u0142owa z tekstu AI, czas m\xF3wienia z czas\xF3w s\u0142\xF3w (przerwy < 0,3 s wliczone do mowy). Mediany."
  };
}
__name(txSpeechRate, "txSpeechRate");
__name2(txSpeechRate, "txSpeechRate");
async function txSpeechRateRecord(env, sid, rid) {
  await ensureTranscriptTables(env);
  const ri = await txRecordInfo(env, rid);
  if (!ri.uid || String(ri.uid) !== String(sid)) return { ok: false, error: "Nie twoje nagranie" };
  const monolog = txIsMonolog(ri.category);
  const t = await env.DB.prepare("SELECT dur, words, speakers, status FROM record_transcript WHERE ns_record_id = ?").bind(String(rid)).first();
  if (!t || !(Number(t.dur) > 0)) {
    try {
      await txPrioAdd(env, sid);
    } catch (e) {
    }
    return { ok: true, pending: true, monolog, category: ri.category || "" };
  }
  let words = [], sp = null;
  try {
    words = JSON.parse(t.words || "[]");
  } catch (e) {
  }
  try {
    sp = t.speakers ? JSON.parse(t.speakers) : null;
  } catch (e) {
  }
  const voices = sp && sp.length === words.length ? new Set(sp).size : 1;
  const m = srMeasure(words, t.dur, 20);
  return Object.assign({ ok: true, record_id: String(rid), category: ri.category || "", monolog: monolog && voices < 2, voices, short: !m && (Number(t.dur) || 0) < 20 }, m || { dur: Number(t.dur) || 0 });
}
__name(txSpeechRateRecord, "txSpeechRateRecord");
__name2(txSpeechRateRecord, "txSpeechRateRecord");
async function txPhoneTranscript(env, sid, b, any) {
  await ensureTranscriptTables(env);
  const rid = String(b.record_id || "");
  if (!rid) return { ok: false, error: "Brak record_id" };
  const ri = await txRecordInfo(env, rid);
  if (!ri.uid || !any && String(ri.uid) !== String(sid)) return { ok: false, error: "Nie twoje nagranie" };
  const text = String(b.text || "").replace(/\s+/g, " ").trim().slice(0, 2e4);
  if (!text) return { ok: false, error: "Pusty tekst" };
  const words = [];
  for (const w of Array.isArray(b.words) ? b.words.slice(0, 6e3) : []) {
    if (Array.isArray(w) && w.length >= 3 && isFinite(Number(w[1])) && isFinite(Number(w[2]))) words.push([String(w[0] || "").slice(0, 60), Math.round(Number(w[1]) * 100) / 100, Math.round(Number(w[2]) * 100) / 100]);
  }
  const dur = Math.max(0, Math.min(36e3, Number(b.dur) || 0));
  const cur = await env.DB.prepare("SELECT status, model, src_file FROM record_transcript WHERE ns_record_id = ?").bind(rid).first();
  if (cur && cur.status === "ok" && !/^android-asr/.test(String(cur.model || "")) && (!ri.file || !cur.src_file || cur.src_file === ri.file))
    return { ok: true, skipped: "whisper" };
  const now = Math.floor(Date.now() / 1e3);
  await env.DB.prepare(`INSERT INTO record_transcript (ns_record_id, ns_student_id, status, text, lang, dur, words, speakers, diar, spk_names, spk_src, model, error, created_at) VALUES (?, ?, 'ok', ?, ?, ?, ?, NULL, NULL, NULL, NULL, ?, NULL, ?)
    ON CONFLICT(ns_record_id) DO UPDATE SET status='ok', text=excluded.text, lang=excluded.lang, dur=excluded.dur, words=excluded.words, speakers=NULL, diar=NULL, spk_names=NULL, spk_src=NULL, model=excluded.model, error=NULL, created_at=excluded.created_at`).bind(rid, String(ri.uid), text, String(b.lang || "pl").slice(0, 8), dur, JSON.stringify(words), "android-asr|" + TX_VERSION, now).run();
  await env.DB.prepare("UPDATE record_transcript SET src_file = ?, fixed = NULL, summary = NULL, edited_by = NULL, spk_people = NULL WHERE ns_record_id = ?").bind(ri.file || null, rid).run();
  try {
    await env.DB.prepare("DELETE FROM phone_pending WHERE ns_record_id = ?").bind(rid).run();
  } catch (e) {
  }
  return { ok: true, saved: true, words: words.length };
}
__name(txPhoneTranscript, "txPhoneTranscript");
__name2(txPhoneTranscript, "txPhoneTranscript");
async function txPhonePending(env, sid, b) {
  const rid = String(b.record_id || "");
  if (!rid) return { ok: false, error: "Brak record_id" };
  const ri = await txRecordInfo(env, rid);
  if (!ri.uid || String(ri.uid) !== String(sid)) return { ok: false, error: "Nie twoje nagranie" };
  await env.DB.prepare("CREATE TABLE IF NOT EXISTS phone_pending (ns_record_id TEXT PRIMARY KEY, at INTEGER)").run();
  await env.DB.prepare("INSERT INTO phone_pending (ns_record_id, at) VALUES (?, ?) ON CONFLICT(ns_record_id) DO UPDATE SET at = excluded.at").bind(rid, Math.floor(Date.now() / 1e3)).run();
  return { ok: true };
}
__name(txPhonePending, "txPhonePending");
__name2(txPhonePending, "txPhonePending");
async function translatorAuth(env, token, email, server) {
  const who = await contactAuth(env, token, email, server || "new");
  if (!who) return null;
  if (!who.forbidden) return { ok: true };
  const em = String(email || "").toLowerCase();
  if (String(env.TRANSLATOR_EMAILS || "").toLowerCase().split(/[,;\s]+/).filter(Boolean).indexOf(em) >= 0) return { ok: true };
  const u = await nsUserByEmail(env, email);
  if (!u || !u.id) return { ok: false };
  let fn = "", ln = "";
  try {
    const pg = new Client({ connectionString: env.HYPERDRIVE.connectionString });
    await pg.connect();
    try {
      const r = (await pg.query("SELECT first_name, last_name FROM users WHERE id::text = $1", [String(u.id)])).rows[0] || {};
      fn = r.first_name || "";
      ln = r.last_name || "";
    } finally {
      try {
        await pg.end();
      } catch (e) {
      }
    }
  } catch (e) {
  }
  return { ok: await txIsTrainerAcc(env, String(u.id), fn, ln, em) };
}
__name(translatorAuth, "translatorAuth");
__name2(translatorAuth, "translatorAuth");
async function txPhoneQueue(env, limit, all) {
  await ensureTranscriptTables(env);
  await env.DB.prepare("CREATE TABLE IF NOT EXISTS phone_pending (ns_record_id TEXT PRIMARY KEY, at INTEGER)").run();
  limit = Math.max(1, Math.min(10, limit || 3));
  const pg = new Client({ connectionString: env.HYPERDRIVE.connectionString });
  await pg.connect();
  let rows = [];
  try {
    rows = (await pg.query(
      `SELECT r.id::text AS id, r.user_id::text AS user_id, r.record_file, u.country, COALESCE(rct.name, rc.name) AS category, ${TX_PATH_SQL_COLS} FROM records r
       LEFT JOIN users u ON u.id = r.user_id
       LEFT JOIN record_categories rc ON rc.id = r.record_category_id
       LEFT JOIN record_category_translations rct ON rct.record_category_id = rc.id AND rct.locale = 'pl'
       WHERE r.deleted_at IS NULL AND r.record_file IS NOT NULL ${all ? "" : "AND r.created_at > now() - interval '90 days'"}
       ORDER BY r.created_at DESC LIMIT ${all ? 2e4 : 600}`
    )).rows;
  } finally {
    try {
      await pg.end();
    } catch (e) {
    }
  }
  const nowS = Math.floor(Date.now() / 1e3);
  if (all) {
    const prio = await txPrioGet(env);
    rows.sort((a, b) => (prio[b.user_id] ? 1 : 0) - (prio[a.user_id] ? 1 : 0));
  }
  const state = {};
  for (let i = 0; i < rows.length; i += 90) {
    const ch = rows.slice(i, i + 90).map((r) => r.id);
    const rs = await env.DB.prepare(`SELECT ns_record_id, status FROM record_transcript WHERE ns_record_id IN (${ch.map(() => "?").join(",")})`).bind(...ch).all();
    for (const x of rs.results || []) state[x.ns_record_id] = all && x.status === "error" ? null : x.status;
    const pp = await env.DB.prepare(`SELECT ns_record_id FROM phone_pending WHERE at > ? AND ns_record_id IN (${ch.map(() => "?").join(",")})`).bind(nowS - 1800, ...ch).all();
    for (const x of pp.results || []) state[x.ns_record_id] = state[x.ns_record_id] || "pending";
  }
  const pathTpl = await txPathTpl(env);
  const out = [];
  let waiting = 0;
  for (const r of rows) {
    if (state[r.id]) continue;
    waiting++;
    if (out.length >= limit) continue;
    out.push({
      record_id: r.id,
      student_id: r.user_id,
      category: r.category || "",
      lang: txLangForCountry(r.country) || "pl",
      file: r.record_file,
      audio_url: nsServerBase("new") + txPathFill(pathTpl, txPathVars(r, r.id, r.user_id, r.record_file))
    });
  }
  if (out.length) await env.DB.batch(out.map((x) => env.DB.prepare("INSERT INTO phone_pending (ns_record_id, at) VALUES (?, ?) ON CONFLICT(ns_record_id) DO UPDATE SET at = excluded.at").bind(x.record_id, nowS)));
  return { ok: true, items: out, waiting };
}
__name(txPhoneQueue, "txPhoneQueue");
__name2(txPhoneQueue, "txPhoneQueue");
var SR_MOOD = { luz: 1, komfortowo: 2, neutralnie: 3, niekomfortowo: 4, stres: 5 };
var SR_POLITE = [
  ["powitanie", /(?<![\p{L}])(dzień dobry|dobry wieczór|cześć|witam|dobrý den|ahoj|hello)(?![\p{L}])/giu],
  ["prosz\u0119", /(?<![\p{L}])(proszę|poproszę|prosím|please)(?![\p{L}])/giu],
  ["dzi\u0119kuj\u0119", /(?<![\p{L}])(dziękuję|dzięki|děkuji|děkuju|ďakujem|thank you|thanks)(?![\p{L}])/giu],
  ["przepraszam", /(?<![\p{L}])(przepraszam|promiňte|prepáčte|sorry|excuse me)(?![\p{L}])/giu],
  ["po\u017Cegnanie", /(?<![\p{L}])(do widzenia|do zobaczenia|na shledanou|dovidenia|goodbye|bye)(?![\p{L}])/giu]
];
function srMedian(a) {
  if (!a.length) return 0;
  const b = a.slice().sort((x, y) => x - y);
  const m = Math.floor(b.length / 2);
  return b.length % 2 ? b[m] : (b[m - 1] + b[m]) / 2;
}
__name(srMedian, "srMedian");
__name2(srMedian, "srMedian");
function srTokens(t) {
  return String(t || "").toLowerCase().replace(/[^\p{L}\s-]/gu, " ").split(/\s+/).filter((w) => w && w.length > 0);
}
__name(srTokens, "srTokens");
__name2(srTokens, "srTokens");
function srFixText(t) {
  if (typeof t !== "string" || !t) return t;
  var M = ["stycznia", "lutego", "marca", "kwietnia", "maja", "czerwca", "lipca", "sierpnia", "wrze\u015Bnia", "pa\u017Adziernika", "listopada", "grudnia"];
  var cap = /* @__PURE__ */ __name(function(src, out) {
    return /^[A-ZŻŹĆĄŚĘŁÓŃ]/.test(src) ? out.charAt(0).toUpperCase() + out.slice(1) : out;
  }, "cap");
  var R = [
    [/\b(brak|bez|podczas|do|od|po|z powodu|wobec)\s+blokady\b(?!\s+mowy)/gi, "$1 bloku mowy"],
    [/staropolskich\s+blokad\b/gi, "blok\xF3w mowy"],
    [/staropolskie\s+blokady\b/gi, "bloki mowy"],
    [/staropolskimi\s+blokadami\b/gi, "blokami mowy"],
    [/staropolskiej\s+blokady\b/gi, "bloku mowy"],
    [/staropolsk[aą]\s+blokad[aęą]\b/gi, "blok mowy"],
    [/blokadami\b(?!\s+mowy)/gi, "blokami mowy"],
    [/blokadach\b(?!\s+mowy)/gi, "blokach mowy"],
    [/blokad\b(?!\s+mowy)/gi, "blok\xF3w mowy"],
    [/blokady\b(?!\s+mowy)/gi, "bloki mowy"],
    [/blokad[aęą]\b(?!\s+mowy)/gi, "blok mowy"],
    [/blokadzie\b(?!\s+mowy)/gi, "bloku mowy"],
    [/blokadami mowy/gi, "blokami mowy"],
    [/blokadach mowy/gi, "blokach mowy"],
    [/blokad mowy/gi, "blok\xF3w mowy"],
    [/blokady mowy/gi, "bloki mowy"],
    [/blokad[aęą] mowy/gi, "blok mowy"],
    [/staropolskiej\b/gi, "starej"],
    [/staropolsk[aą]\b/gi, "stara"],
    [/staropolskie\b/gi, "stare"],
    [/staropolskich\b/gi, "starych"],
    [/staropolskim\b/gi, "starym"],
    [/staropolski\b/gi, "stary"]
  ];
  R.forEach(function(r) {
    t = t.replace(r[0], function(m, a) {
      var out = r[1].indexOf("$1") >= 0 ? r[1].replace("$1", a) : r[1];
      return r[1].indexOf("$1") >= 0 ? out : cap(m, out);
    });
  });
  t = t.replace(/\b(20\d\d)-(0[1-9]|1[0-2])-(0[1-9]|[12]\d|3[01])\b/g, function(m, y, mo, d) {
    return +d + " " + M[+mo - 1] + " " + y;
  });
  t = t.replace(/(^|[^\d.,])(0?[1-9]|[12]\d|3[01])\.(0[1-9]|1[0-2])\.(20\d\d)(?![\d])/g, function(m, p, d, mo, y) {
    return p + +d + " " + M[+mo - 1] + " " + y;
  });
  t = t.replace(/(^|[^\d.,:])(0?[1-9]|[12]\d|3[01])[. ](0[1-9]|1[0-2])(?!\d|[.,:]\d|\s*(?:min|h\b|s\b|%|sek|słów|nagr|razy|zł|dni|osób|rozm|tel))/g, function(m, p, d, mo, off, all) {
    if (/(^|[^a-ząćęłńóśźż])(godz\.?|godzin\w*|o|ok\.|około)\s*$/i.test(all.slice(Math.max(0, off - 9), off + p.length))) return m;
    return p + +d + " " + M[+mo - 1];
  });
  return t;
}
__name(srFixText, "srFixText");
function srFixDeep(o) {
  if (typeof o === "string") return srFixText(o);
  if (Array.isArray(o)) return o.map(srFixDeep);
  if (o && typeof o === "object") {
    var r = {};
    for (var k in o) r[k] = srFixDeep(o[k]);
    return r;
  }
  return o;
}
__name(srFixDeep, "srFixDeep");
async function txStudentReport(env, sid, days, extra) {
  await ensureTranscriptTables(env);
  try {
    await env.DB.prepare("CREATE TABLE IF NOT EXISTS student_report (ns_student_id TEXT, period TEXT, report TEXT, created_at INTEGER, PRIMARY KEY (ns_student_id, period))").run();
  } catch (e) {
  }
  days = Math.max(7, Math.min(366, parseInt(days, 10) || 30));
  const now = /* @__PURE__ */ new Date();
  let from = new Date(now - (days - 1) * 864e5).toISOString().slice(0, 10), to = now.toISOString().slice(0, 10), rangeLabel = "ostatnie " + days + " dni";
  const rg = extra && extra.range;
  if (rg && /^\d{4}-\d{2}-\d{2}$/.test(String(rg.from || "")) && /^\d{4}-\d{2}-\d{2}$/.test(String(rg.to || "")) && rg.from <= rg.to) {
    from = rg.from;
    to = rg.to > to ? to : rg.to;
    days = Math.max(1, Math.round((new Date(to) - new Date(from)) / 864e5) + 1);
    rangeLabel = String(rg.label || "okres harmonogramu").slice(0, 80);
  }
  const pg = new Client({ connectionString: env.HYPERDRIVE.connectionString });
  await pg.connect();
  let me = {}, own = [];
  const catSql = `LEFT JOIN record_categories rc ON rc.id = r.record_category_id LEFT JOIN record_category_translations rct ON rct.record_category_id = rc.id AND rct.locale = 'pl'`;
  try {
    me = (await pg.query("SELECT id::text AS id, first_name, last_name, country FROM users WHERE id::text = $1", [String(sid)])).rows[0] || {};
    own = (await pg.query(`SELECT r.id::text AS id, COALESCE(r.date, r.created_at::date) AS d, r.record_file, r.review_details, COALESCE(rct.name, rc.name) AS category
      FROM records r ${catSql} WHERE r.user_id::text = $1 AND r.deleted_at IS NULL AND r.record_file IS NOT NULL
      AND COALESCE(r.date, r.created_at::date) BETWEEN $2::date AND $3::date ORDER BY r.created_at`, [String(sid), from, to])).rows;
  } catch (e) {
    try {
      await pg.end();
    } catch (e2) {
    }
    throw e;
  }
  const first = String(me.first_name || "").trim(), last = String(me.last_name || "").trim();
  const full = (first + " " + last).trim().toLowerCase();
  const tset = await txTrainerSet(env);
  const conds = ["spk_people LIKE ?"], binds = ['%"' + String(sid) + '"%'];
  if (first && last) {
    conds.push("LOWER(COALESCE(text,'') || ' ' || COALESCE(fixed,'') || ' ' || COALESCE(spk_names,'') || ' ' || COALESCE(summary,'')) LIKE ?");
    binds.push("%" + full + "%");
  }
  let extraT = [];
  try {
    extraT = (await env.DB.prepare(`SELECT * FROM record_transcript WHERE status = 'ok' AND COALESCE(ns_student_id,'') != ? AND (${conds.join(" OR ")}) LIMIT 400`).bind(String(sid), ...binds).all()).results || [];
    if (first) {
      const trIds = [...tset];
      const tr = (await env.DB.prepare(`SELECT * FROM record_transcript WHERE status = 'ok' AND COALESCE(ns_student_id,'') != ? AND LOWER(COALESCE(spk_names,'') || ' ' || COALESCE(summary,'')) LIKE ? LIMIT 400`).bind(String(sid), "%" + first.toLowerCase() + "%").all()).results || [];
      extraT = extraT.concat(tr.filter((x) => !extraT.some((y) => y.ns_record_id === x.ns_record_id)).map((x) => ({ ...x, _byFirst: true, _trIds: trIds })));
    }
  } catch (e) {
  }
  let extraMeta = {};
  if (extraT.length) {
    try {
      const rows = (await pg.query(`SELECT r.id::text AS id, COALESCE(r.date, r.created_at::date) AS d, r.record_file, r.review_details, COALESCE(rct.name, rc.name) AS category,
          u.id::text AS uid, u.first_name, u.last_name, u.email FROM records r JOIN users u ON u.id = r.user_id ${catSql}
        WHERE r.id::text = ANY($1) AND r.deleted_at IS NULL AND COALESCE(r.date, r.created_at::date) BETWEEN $2::date AND $3::date`, [extraT.map((x) => x.ns_record_id), from, to])).rows;
      for (const m of rows) extraMeta[m.id] = m;
    } catch (e) {
    }
  }
  let nsDiary = [];
  try {
    nsDiary = (await pg.query(`SELECT d.date::text AS d, to_jsonb(d) AS j FROM diaries d WHERE d.user_id::text = $1 AND d.deleted_at IS NULL AND d.date BETWEEN $2::date AND $3::date ORDER BY d.date`, [String(sid), from, to])).rows;
  } catch (e) {
  }
  try {
    await pg.end();
  } catch (e) {
  }
  const ownT = {};
  const ownIds = own.filter((r) => !isNotCountedReview(r.review_details)).map((r) => r.id);
  for (let i = 0; i < ownIds.length; i += 90) {
    const ch = ownIds.slice(i, i + 90);
    const rs = await env.DB.prepare(`SELECT * FROM record_transcript WHERE status = 'ok' AND ns_record_id IN (${ch.map(() => "?").join(",")})`).bind(...ch).all();
    for (const x of rs.results || []) ownT[x.ns_record_id] = x;
  }
  const J = /* @__PURE__ */ __name((v, d) => {
    try {
      return v ? JSON.parse(v) : d;
    } catch (e) {
      return d;
    }
  }, "J");
  const ymd = /* @__PURE__ */ __name((d) => (d instanceof Date ? d.toISOString() : String(d || "")).slice(0, 10), "ymd");
  const recs = [];
  const build = /* @__PURE__ */ __name((meta, t, isOwn) => {
    const arr = /* @__PURE__ */ __name((x) => Array.isArray(x) ? x : null, "arr"), obj = /* @__PURE__ */ __name((x) => x && typeof x === "object" && !Array.isArray(x) ? x : {}, "obj");
    const words = (arr(J(t.words, [])) || []).filter((w) => Array.isArray(w)), sp = arr(J(t.speakers, null)), names = obj(J(t.spk_names, {})), people = obj(J(t.spk_people, {})), fixed = arr(J(t.fixed, null)) || null;
    const spOk = sp && sp.length === words.length;
    let idx = null;
    for (const k of Object.keys(people)) if (String(people[k]) === String(sid)) idx = Number(k);
    if (idx === null && first) for (const k of Object.keys(names)) {
      const n = String(names[k] || "").toLowerCase();
      if (n.indexOf(first.toLowerCase()) >= 0 && (!last || n.indexOf(last.toLowerCase()) >= 0 || !isOwn)) {
        idx = Number(k);
        break;
      }
    }
    const trainerAcc = !isOwn && (tset.has(String(meta.uid)) || TX_TRAINER_NAME_RX.test(((meta.first_name || "") + " " + (meta.last_name || "")).trim()) || txTrainerEmails(env).indexOf(String(meta.email || "").toLowerCase()) >= 0);
    if (idx === null && isOwn && spOk && t.spk_src === "ai-text") idx = 0;
    const persons = spOk ? new Set(sp).size : 1;
    const solo = persons < 2 && isOwn;
    let kText = "";
    if (solo) kText = fixed && fixed.length ? fixed.map((f) => f[1]).join(" ") : words.map((w) => w[0]).join(" ");
    else if (idx !== null) kText = fixed && fixed.length && fixed.some((f) => f[0] !== null && f[0] !== void 0) ? fixed.filter((f) => f[0] === idx).map((f) => f[1]).join(" ") : spOk ? words.filter((w, i) => sp[i] === idx).map((w) => w[0]).join(" ") : "";
    const plain = words.map((w) => w[0]).join(" ") + " " + (t.text || "");
    const label = /* @__PURE__ */ __name((k) => k === idx ? "KURSANT " + (first || "") : people[k] !== void 0 ? names[k] || "Osoba " + (Number(k) + 1) : names[k] ? txCleanRole(String(names[k]), plain) : "Osoba " + (Number(k) + 1), "label");
    let dialog = "";
    if (fixed && fixed.length) {
      let cur, buf = [], out = [];
      for (const [k, tx] of fixed) {
        if (k !== cur) {
          if (buf.length) out.push((cur === null || cur === void 0 ? "" : label(cur) + ": ") + buf.join(" "));
          buf = [];
          cur = k;
        }
        buf.push(tx);
      }
      if (buf.length) out.push((cur === null || cur === void 0 ? "" : label(cur) + ": ") + buf.join(" "));
      dialog = out.join("\n");
    } else if (spOk) {
      let cur = null, buf = [], out = [];
      words.forEach((w, i) => {
        if (sp[i] !== cur) {
          if (buf.length) out.push(label(cur) + ": " + buf.join(" "));
          buf = [];
          cur = sp[i];
        }
        buf.push(w[0]);
      });
      if (buf.length) out.push(label(cur) + ": " + buf.join(" "));
      dialog = out.join("\n");
    } else dialog = String(t.text || "");
    let ex = 0;
    if (spOk) {
      for (let i = 1; i < sp.length; i++) if (sp[i] !== sp[i - 1]) ex++;
    }
    let pauses = 0, longest = 0;
    for (let i = 1; i < words.length; i++) {
      const g = words[i][1] - words[i - 1][2];
      if (g >= 1) {
        pauses++;
        if (g > longest) longest = g;
      }
    }
    const file = String(meta.record_file || "");
    const tm = file.match(/_(\d{2})h-(\d{2})m-(\d{2})s_/), gps = file.match(/_GPS(-?\d+\.\d+)_(-?\d+\.\d+)/), ter2 = file.match(/teren-(\d+(?:\.\d+)?)km/), dom = /_dom(\.|_|$)/.test(file);
    const core = file.replace(/\.(mp3|m4a|wav|ogg|webm|aac)$/i, "").replace(/_(GPS|teren|dom)[\s\S]*$/i, "");
    const mm = core.match(/_-([A-Za-z][A-Za-z0-9]*)-([a-zA-ZąćęłńóśźżĄĆĘŁŃÓŚŹŻ]+)$/);
    const moodW = isOwn && mm ? mm[2].toLowerCase() : null, moodL = moodW ? SR_MOOD[moodW] || null : null;
    const otherIdx = Object.keys(names).concat(Object.keys(people)).map(Number).filter((k, i, a) => k !== idx && a.indexOf(k) === i);
    const otherSids = Object.keys(people).filter((k) => Number(k) !== idx && String(people[k]) !== String(sid)).map((k) => String(people[k]));
    const withTrainer = trainerAcc || otherIdx.some((k) => /tren|trainer|tréner/i.test(String(names[k] || "")));
    const withStudent = otherSids.length > 0 || !trainerAcc && otherIdx.some((k) => /kursant|kurzist/i.test(String(names[k] || "")));
    recs.push({
      rid: t.ns_record_id,
      own: isOwn,
      mood: moodW,
      level: moodL,
      otherSids,
      withTrainer,
      withStudent,
      owner: isOwn ? "" : ((meta.first_name || "") + " " + (meta.last_name || "")).trim(),
      trainerAcc,
      d: ymd(meta.d),
      cat: meta.category || "(bez kategorii)",
      dur: Number(t.dur) || 0,
      nWords: words.length,
      kText,
      solo,
      persons,
      idx,
      ex,
      pauses,
      longest,
      hour: tm ? Number(tm[1]) : null,
      gps: gps ? [Number(gps[1]), Number(gps[2])] : null,
      teren: ter2 ? Number(ter2[1]) : null,
      dom,
      partners: Object.keys(names).filter((k) => Number(k) !== idx && people[k] === void 0).flatMap((k) => txCleanRole(String(names[k]), words.map((w) => w[0]).join(" ") + " " + (t.text || "")).split(/\s*,\s*/)).map((n) => n.toLowerCase().replace(/^przechodnia$/, "przechodzie\u0144").replace(/^przechodnie$/, "przechodzie\u0144").trim()).filter((n) => n && !/^osoba|^rozmówca|^kursant|^drugi|^trzeci/i.test(n)),
      dialog
    });
  }, "build");
  for (const r of own) {
    const t = ownT[r.id];
    if (t) {
      try {
        build(r, t, true);
      } catch (e) {
      }
    }
  }
  const ownNoText = own.filter((r) => !ownT[r.id]).length;
  if (ownNoText) {
    try {
      await txPrioAdd(env, sid);
    } catch (e) {
    }
  }
  for (const t of extraT) {
    const m = extraMeta[t.ns_record_id];
    if (!m) continue;
    if (t._byFirst && !(t._trIds || []).includes(String(m.uid)) && !TX_TRAINER_NAME_RX.test(((m.first_name || "") + " " + (m.last_name || "")).trim()) && txTrainerEmails(env).indexOf(String(m.email || "").toLowerCase()) < 0) continue;
    try {
      build(m, t, false);
    } catch (e) {
    }
  }
  if (!recs.length) return { ok: false, error: "Brak przepisanych nagra\u0144 kursanta w tym okresie" };
  const ownR = recs.filter((r) => r.own), durs = recs.map((r) => r.dur);
  const totalDur = durs.reduce((s, x) => s + x, 0);
  const byLen = recs.slice().sort((a, b) => a.dur - b.dur);
  const cats = {};
  for (const r of recs) {
    const c = cats[r.cat] = cats[r.cat] || { cat: r.cat, n: 0, dur: 0, tokens: [], ex: [], words: 0 };
    c.n++;
    c.dur += r.dur;
    c.words += r.nWords;
    if (r.persons >= 2) c.ex.push(r.ex);
    c.tokens.push(...srTokens(r.kText));
  }
  const catRows = Object.values(cats).map((c) => ({
    cat: c.cat,
    n: c.n,
    dur: Math.round(c.dur),
    avg: Math.round(c.dur / c.n * 10) / 10,
    share: totalDur ? Math.round(c.dur * 1e3 / totalDur) / 10 : 0,
    ttr: c.tokens.length >= 30 ? Math.round(new Set(c.tokens).size * 100 / c.tokens.length) / 100 : null,
    kWords: c.tokens.length,
    secPerWord: c.words ? Math.round(c.dur / c.words * 100) / 100 : null,
    exAvg: c.ex.length ? Math.round(c.ex.reduce((s, x) => s + x, 0) / c.ex.length * 10) / 10 : null
  })).sort((a, b) => b.dur - a.dur);
  const daysM = {};
  for (const r of recs) {
    const x = daysM[r.d] = daysM[r.d] || { d: r.d, n: 0, dur: 0, cats: {} };
    x.n++;
    x.dur += r.dur;
    x.cats[r.cat] = (x.cats[r.cat] || 0) + r.dur;
  }
  const dayRows = Object.values(daysM).sort((a, b) => a.d.localeCompare(b.d)).map((x) => ({ d: x.d, n: x.n, dur: Math.round(x.dur), avg: Math.round(x.dur / x.n * 10) / 10, main: Object.keys(x.cats).sort((a, b) => x.cats[b] - x.cats[a])[0] }));
  const allTok = recs.flatMap((r) => srTokens(r.kText));
  const polite = {};
  const kAll = recs.map((r) => r.kText).join(" \n ");
  for (const [n, rx] of SR_POLITE) polite[n] = (kAll.match(rx) || []).length;
  const prolong = {};
  for (const w of allTok) if (/([aąeęioóuy])\1{2,}/u.test(w)) prolong[w] = (prolong[w] || 0) + 1;
  const monoRows = ownR.filter((r) => r.solo && /monolog|czytani|podsumowanie dnia/i.test(r.cat)).map((r) => ({ d: r.d, dur: Math.round(r.dur * 10) / 10, words: r.nWords }));
  const hours = { "rano (6\u20139)": 0, "przedpo\u0142udnie (10\u201312)": 0, "popo\u0142udnie (13\u201317)": 0, "wiecz\xF3r (18\u201321)": 0, "noc (22\u20135)": 0 };
  for (const r of recs) {
    if (r.hour === null) continue;
    const h = r.hour;
    const k = h >= 6 && h <= 9 ? "rano (6\u20139)" : h >= 10 && h <= 12 ? "przedpo\u0142udnie (10\u201312)" : h >= 13 && h <= 17 ? "popo\u0142udnie (13\u201317)" : h >= 18 && h <= 21 ? "wiecz\xF3r (18\u201321)" : "noc (22\u20135)";
    hours[k]++;
  }
  const gpsSet = new Set(recs.filter((r) => r.gps).map((r) => r.gps.map((x) => x.toFixed(3)).join(",")));
  const ter = recs.filter((r) => r.teren !== null).map((r) => r.teren);
  const partners = {};
  for (const r of recs) for (const p of r.partners) partners[p] = (partners[p] || 0) + 1;
  const isConv = /* @__PURE__ */ __name((r) => !r.solo || r.persons >= 2 || !/monolog|czytani|podsumowanie dnia/i.test(r.cat), "isConv");
  const r1 = /* @__PURE__ */ __name((x) => Math.round(x * 10) / 10, "r1");
  const wkKey = /* @__PURE__ */ __name((d) => {
    const x = /* @__PURE__ */ new Date(d + "T12:00:00Z");
    x.setUTCDate(x.getUTCDate() - (x.getUTCDay() + 6) % 7);
    return x.toISOString().slice(0, 10);
  }, "wkKey");
  const wks = {};
  for (const r of recs) {
    const w = wks[wkKey(r.d)] = wks[wkKey(r.d)] || { week: wkKey(r.d), n: 0, dur: 0, convN: 0, convDur: 0, ml: [] };
    w.n++;
    w.dur += r.dur;
    if (isConv(r)) {
      w.convN++;
      w.convDur += r.dur;
    }
    if (r.level) w.ml.push(r.level);
  }
  const weekRows = Object.values(wks).sort((a, b) => a.week.localeCompare(b.week)).map((w) => ({ week: w.week, n: w.n, min: r1(w.dur / 60), convN: w.convN, convMin: r1(w.convDur / 60), moodAvg: w.ml.length ? Math.round(w.ml.reduce((a, b) => a + b, 0) / w.ml.length * 10) / 10 : null }));
  const mid = new Date(((/* @__PURE__ */ new Date(from + "T00:00:00Z")).getTime() + (/* @__PURE__ */ new Date(to + "T00:00:00Z")).getTime()) / 2).toISOString().slice(0, 10);
  const half = /* @__PURE__ */ __name((arr, dLen) => {
    const conv = arr.filter(isConv), ml = arr.filter((r) => r.level).map((r) => r.level), ad = new Set(arr.map((r) => r.d)).size;
    return {
      days: dLen,
      n: arr.length,
      min: r1(arr.reduce((s, r) => s + r.dur, 0) / 60),
      convN: conv.length,
      convMin: r1(conv.reduce((s, r) => s + r.dur, 0) / 60),
      convAvgMin: conv.length ? r1(conv.reduce((s, r) => s + r.dur, 0) / 60 / conv.length) : null,
      activeDays: ad,
      cats: new Set(arr.map((r) => r.cat)).size,
      moodAvg: ml.length ? r1(ml.reduce((a, b) => a + b, 0) / ml.length) : null,
      moodGood: ml.length ? Math.round(ml.filter((x) => x <= 2).length * 100 / ml.length) : null,
      withStudents: arr.filter((r) => r.withStudent).length,
      kWords: arr.reduce((s, r) => s + srTokens(r.kText).length, 0)
    };
  }, "half");
  const dA = Math.round((new Date(mid) - new Date(from)) / 864e5) + 1, dB = days - dA;
  const growth = { mid, a: half(recs.filter((r) => r.d <= mid), dA), b: half(recs.filter((r) => r.d > mid), dB) };
  const convCats = {};
  for (const r of recs.filter(isConv)) {
    const c = convCats[r.cat] = convCats[r.cat] || { cat: r.cat, n: 0, dur: 0, ex: [], byDay: {} };
    c.n++;
    c.dur += r.dur;
    if (r.persons >= 2) c.ex.push(r.ex);
    c.byDay[r.d] = (c.byDay[r.d] || 0) + 1;
  }
  const convRows = Object.values(convCats).map((c) => {
    const best = Object.keys(c.byDay).sort((a, b) => c.byDay[b] - c.byDay[a])[0];
    return { cat: c.cat, n: c.n, min: r1(c.dur / 60), avgMin: r1(c.dur / 60 / c.n), exAvg: c.ex.length ? r1(c.ex.reduce((a, b) => a + b, 0) / c.ex.length) : null, bestDay: best ? { d: best, n: c.byDay[best] } : null };
  }).sort((a, b) => b.min - a.min);
  const phoneR = recs.filter((r) => /phone|telefon/i.test(r.cat));
  const phGroup = /* @__PURE__ */ __name((r) => r.withTrainer ? "trener" : r.withStudent || /kursant/i.test(r.cat) && !/miast|rodzin|znajom/i.test(r.cat) ? "kursant" : /miast|urz|firm|sklep|usług/i.test(r.cat) ? "miasto" : /rodzin|znajom|przyjaci/i.test(r.cat) ? "rodzina" : "inne", "phGroup");
  const phG = {};
  for (const r of phoneR) {
    const g = phG[phGroup(r)] = phG[phGroup(r)] || { n: 0, dur: 0 };
    g.n++;
    g.dur += r.dur;
  }
  const phone = {
    n: phoneR.length,
    min: r1(phoneR.reduce((s, r) => s + r.dur, 0) / 60),
    trener: phoneR.filter((r) => r.withTrainer).length,
    kursanci: (phG.kursant || {}).n || 0,
    inne: phoneR.length - phoneR.filter((r) => r.withTrainer).length - ((phG.kursant || {}).n || 0),
    groups: ["trener", "kursant", "miasto", "rodzina", "inne"].filter((k) => phG[k]).map((k) => ({ g: k, n: phG[k].n, min: r1(phG[k].dur / 60) }))
  };
  const sidCount = {};
  for (const r of recs) for (const x of r.otherSids) sidCount[x] = (sidCount[x] || 0) + 1;
  let studentNames = {};
  if (Object.keys(sidCount).length) {
    const pg2 = new Client({ connectionString: env.HYPERDRIVE.connectionString });
    try {
      await pg2.connect();
      for (const u of (await pg2.query("SELECT id::text AS id, first_name, last_name FROM users WHERE id::text = ANY($1)", [Object.keys(sidCount)])).rows) studentNames[u.id] = ((u.first_name || "") + " " + (u.last_name || "")).trim();
    } catch (e) {
    }
    try {
      await pg2.end();
    } catch (e) {
    }
  }
  const contacts = {
    withTrainer: recs.filter((r) => r.withTrainer).length,
    withStudents: recs.filter((r) => r.withStudent).length,
    students: Object.keys(sidCount).sort((a, b) => sidCount[b] - sidCount[a]).map((x) => ({ name: studentNames[x] || "kursant", n: sidCount[x] }))
  };
  const moodR = recs.filter((r) => r.level);
  const moodDist = { luz: 0, komfortowo: 0, neutralnie: 0, niekomfortowo: 0, stres: 0 };
  for (const r of moodR) moodDist[r.mood] = (moodDist[r.mood] || 0) + 1;
  const moodCat = {};
  for (const r of moodR) {
    const c = moodCat[r.cat] = moodCat[r.cat] || { cat: r.cat, n: 0, s: 0, good: 0 };
    c.n++;
    c.s += r.level;
    if (r.level <= 2) c.good++;
  }
  const mood = {
    n: moodR.length,
    avg: moodR.length ? r1(moodR.reduce((s, r) => s + r.level, 0) / moodR.length) : null,
    dist: moodDist,
    byCat: Object.values(moodCat).map((c) => ({ cat: c.cat, n: c.n, avg: r1(c.s / c.n), good: Math.round(c.good * 100 / c.n) })).sort((a, b) => b.n - a.n)
  };
  const tasks = (Array.isArray(extra && extra.tasks) ? extra.tasks : []).slice(0, 60).map((x) => ({
    code: String(x.code || ""),
    title: String(x.title || "").slice(0, 120),
    desc: String(x.desc || "").slice(0, 300),
    note: String(x.note || "").slice(0, 300),
    done: x.done_at ? new Date(Number(x.done_at) * (Number(x.done_at) < 1e12 ? 1e3 : 1)).toISOString().slice(0, 10) : null
  })).map((x) => ({ ...x, inPeriod: !!(x.done && x.done >= from && x.done <= to) }));
  const questions = (() => {
    const QW = /^(przepraszam[, ]+)?(czy|gdzie|ile|jak|jaki|jaka|jakie|który|która|które|co|kiedy|dlaczego|czemu|skąd|dokąd|kto|o której|w którą|mogę|można|macie|ma pan|ma pani)\b/i;
    const fold = /* @__PURE__ */ __name((t) => String(t).toLowerCase().normalize("NFD").replace(/[̀-ͯ]/g, "").replace(/ł/g, "l"), "fold");
    const by = {};
    for (const r of recs) {
      if (r.solo) continue;
      const sents = String(r.kText || "").replace(/\s+/g, " ").split(/(?<=[?.!])\s+/);
      for (let t of sents) {
        t = t.trim();
        if (t.length < 6 || t.length > 160) continue;
        const isQ = /\?$/.test(t) || QW.test(t.replace(/[„"]/g, ""));
        if (!isQ) continue;
        const clean = t.replace(/^(yyy|eee|no|a|i|to|halo)[, ]+/i, "").replace(/^(dzień dobry|dobry wieczór|cześć|witam)[,!. ]+/i, "").replace(/(\p{L})\1{2,}/gu, "$1").replace(/\s*\?+$/, "?").replace(/[.!]+$/, "?");
        const key = fold(clean).replace(/^przepraszam[, ]+/, "").replace(/[^\p{L}\p{N} ]+/gu, " ").replace(/\s+/g, " ").trim().split(" ").slice(0, 3).join(" ");
        if (key.split(" ").length < 2) continue;
        if (/\bdawid|^wiesz\b|\b(trenerz|trenera|kursancie)\b/i.test(clean)) continue;
        const c = by[r.cat] = by[r.cat] || { cat: r.cat, n: 0, g: {}, t: {} };
        c.n++;
        const g = c.g[key] = c.g[key] || { q: clean.charAt(0).toUpperCase() + clean.slice(1), n: 0, v: {} };
        g.n++;
        const vk = clean.toLowerCase();
        g.v[vk] = (g.v[vk] || 0) + 1;
        if (g.v[vk] > (g.v[g.q.toLowerCase()] || 0)) g.q = clean.charAt(0).toUpperCase() + clean.slice(1);
        const tw = (fold(clean).replace(/^przepraszam[, ]+/, "").match(/^(czy|gdzie|ile|jak|jaki|jaka|jakie|ktory|ktora|ktore|co|kiedy|dlaczego|skad|dokad|kto|o ktorej|mozna|moge|macie)/) || [])[1];
        if (tw) c.t[tw] = (c.t[tw] || 0) + 1;
      }
    }
    return Object.values(by).filter((c) => c.n >= 2).sort((a, b) => b.n - a.n).slice(0, 10).map((c) => ({ cat: c.cat, n: c.n, types: Object.keys(c.t).sort((a, b) => c.t[b] - c.t[a]).map((k) => ({ w: k, n: c.t[k] })), top: Object.values(c.g).sort((a, b) => b.n - a.n).slice(0, 10).map((g) => ({ q: g.q, n: g.n })) }));
  })();
  const stats = {
    student: (first + " " + last).trim(),
    from,
    to,
    days,
    questions,
    total: {
      n: recs.length,
      own: ownR.length,
      other: recs.length - ownR.length,
      dur: Math.round(totalDur),
      avg: Math.round(totalDur / recs.length * 10) / 10,
      median: Math.round(srMedian(durs) * 10) / 10,
      shortest: { dur: Math.round(byLen[0].dur * 10) / 10, cat: byLen[0].cat, d: byLen[0].d },
      longest: { dur: Math.round(byLen[byLen.length - 1].dur * 10) / 10, cat: byLen[byLen.length - 1].cat, d: byLen[byLen.length - 1].d },
      activeDays: dayRows.length,
      wordsAll: recs.reduce((s, r) => s + r.nWords, 0),
      wordsKursant: allTok.length
    },
    categories: catRows,
    daysTable: dayRows,
    vocab: {
      tokens: allTok.length,
      unique: new Set(allTok).size,
      ttr: allTok.length ? Math.round(new Set(allTok).size * 100 / allTok.length) / 100 : null,
      polite,
      prolongations: Object.keys(prolong).sort((a, b) => prolong[b] - prolong[a]).slice(0, 12).map((w) => ({ w, n: prolong[w] }))
    },
    monologs: monoRows,
    hours,
    gps: { points: gpsSet.size, dom: recs.filter((r) => r.dom).length, terenMin: ter.length ? Math.min(...ter) : null, terenMax: ter.length ? Math.max(...ter) : null },
    partners: Object.keys(partners).sort((a, b) => partners[b] - partners[a]).slice(0, 20).map((p) => ({ p, n: partners[p] })),
    ownWithoutText: ownNoText,
    weeks: weekRows,
    growth,
    conv: convRows,
    phone,
    contacts,
    mood,
    tasks,
    otherRecs: recs.filter((r) => !r.own).map((r) => ({ d: r.d, cat: r.cat, owner: r.owner, dur: Math.round(r.dur), trainerAcc: r.trainerAcc }))
  };
  const budget = 6e4, per = Math.max(250, Math.floor(budget / recs.length));
  const diaryDays = {};
  const skipKey = /(^|_)(id|at|token|uuid|user|created|updated|deleted|reviewed|review|trainer)($|_)|^is_correct$|^date$|^entry_date$/i;
  const nsLbl = { talk_feelings_substantiation: "samopoczucie w rozmowach \u2014 dlaczego", old_speech_usage_comment: "stara mowa \u2014 kiedy", note: "notatka", notes: "notatki", comment: "komentarz", content: "wpis" };
  for (const r of nsDiary) {
    const j = r.j || {}, d = String(r.d).slice(0, 10), o = diaryDays[d] = diaryDays[d] || { d, txt: [], med: null, old: null, sleep: null, tired: null, motiv: null, feel: null };
    if (j.meditation_count != null) o.med = Number(j.meditation_count) || 0;
    if (j.old_speech_usage != null) o.old = !!j.old_speech_usage;
    for (const [k, v] of Object.entries(j)) {
      if (skipKey.test(k) || v == null) continue;
      if (typeof v === "string" && v.trim().length > 2) o.txt.push((nsLbl[k] || k.replace(/_/g, " ")) + ": " + v.trim().slice(0, 400));
    }
  }
  try {
    const du = await env.DB.prepare("SELECT id FROM users WHERE ns_legacy_id = ?").bind(String(sid)).first();
    if (du) {
      const ex = (await env.DB.prepare("SELECT * FROM diary_entries WHERE user_id = ? AND entry_date BETWEEN ? AND ? ORDER BY entry_date").bind(du.id, from, to).all()).results || [];
      const lbl = { feeling: "samopoczucie", feeling_context: "sk\u0105d to samopoczucie", hard_situations: "trudne sytuacje", easy_situations: "\u0142atwe sytuacje", motivation: "motywacja", notes: "notatki", physical_activity: "aktywno\u015B\u0107 fizyczna", tiredness: "zm\u0119czenie", sleep_hours: "sen (h)", coffee_count: "kawy", city: "miasto" };
      for (const e of ex) {
        const d = String(e.entry_date).slice(0, 10), o = diaryDays[d] = diaryDays[d] || { d, txt: [], med: null, old: null, sleep: null, tired: null, motiv: null, feel: null };
        if (e.meditation_count != null && o.med == null) o.med = Number(e.meditation_count) || 0;
        if (e.sleep_hours != null && e.sleep_hours !== "") o.sleep = Number(e.sleep_hours) || null;
        if (e.tiredness != null && e.tiredness !== "") o.tired = Number(e.tiredness) || null;
        if (e.motivation != null && /^\d+(\.\d+)?$/.test(String(e.motivation))) o.motiv = Number(e.motivation);
        if (e.feeling) o.feel = String(e.feeling);
        for (const k of Object.keys(lbl)) {
          const v = e[k];
          if (v != null && String(v).trim() && !(k === "sleep_hours" || k === "coffee_count")) o.txt.push(lbl[k] + ": " + String(v).trim().slice(0, 400));
        }
      }
    }
  } catch (e) {
  }
  const dList = Object.values(diaryDays).sort((a, b) => a.d.localeCompare(b.d));
  const avg = /* @__PURE__ */ __name((arr) => {
    const v = arr.filter((x) => x != null && !isNaN(x));
    return v.length ? r1(v.reduce((a, b) => a + b, 0) / v.length) : null;
  }, "avg");
  const feelC = {};
  dList.forEach((x) => {
    if (x.feel) feelC[x.feel] = (feelC[x.feel] || 0) + 1;
  });
  const diary = {
    days: dList.length,
    of: days,
    med_days: dList.filter((x) => x.med > 0).length,
    med_avg: avg(dList.map((x) => x.med)),
    old_speech_days: dList.filter((x) => x.old === true).length,
    sleep_avg: avg(dList.map((x) => x.sleep)),
    tired_avg: avg(dList.map((x) => x.tired)),
    motiv_avg: avg(dList.map((x) => x.motiv)),
    feelings: Object.keys(feelC).sort((a, b) => feelC[b] - feelC[a]).slice(0, 6).map((k) => ({ f: k, n: feelC[k] })),
    weeks: (() => {
      const w = {};
      dList.forEach((x) => {
        const k = x.d.slice(0, 8) + String(Math.min(4, Math.floor((Number(x.d.slice(8, 10)) - 1) / 7) + 1));
        w[k] = (w[k] || 0) + 1;
      });
      return w;
    })()
  };
  stats.diary = diary;
  stats.range_label = rangeLabel;
  let parentCorpus = "";
  try {
    await pwTables(env);
    const pw = ((await env.DB.prepare("SELECT week, data FROM parent_weekly WHERE ns_student_id = ? AND week >= ? AND week <= ? ORDER BY week").bind(String(sid), pwWeek(from + "T12:00:00Z"), to).all()).results || []).map((r) => Object.assign({ week: r.week }, JSON.parse(r.data || "{}")));
    if (pw.length) {
      const av = /* @__PURE__ */ __name((k) => {
        const a = pw.map((x) => Number(x[k])).filter((x) => x > 0 || k === "days" && x === 0);
        return a.length ? Math.round(a.reduce((s, x) => s + x, 0) / a.length * 10) / 10 : null;
      }, "av");
      const cnt = /* @__PURE__ */ __name((k) => {
        const m = {};
        pw.forEach((x) => (Array.isArray(x[k]) ? x[k] : []).forEach((v) => {
          m[v] = (m[v] || 0) + 1;
        }));
        return Object.keys(m).sort((a, b) => m[b] - m[a]).slice(0, 6).map((v) => ({ v, n: m[v] }));
      }, "cnt");
      stats.parent = {
        n: pw.length,
        flu_avg: av("flu"),
        mood_avg: av("mood"),
        days_avg: av("days"),
        first: pw[0],
        last: pw[pw.length - 1],
        good: cnt("good"),
        hard: cnt("hard"),
        weeks: pw.map((x) => ({ week: x.week, flu: Number(x.flu) || null, mood: Number(x.mood) || null, days: x.days != null ? Number(x.days) : null, old: x.old || "" })),
        calls: pw.filter((x) => x.call === "Tak").length
      };
      parentCorpus = pw.map((x) => "### tydzie\u0144 od " + x.week + ": mowa " + (x.flu || "?") + "/10, stara mowa: " + (x.old || "?") + ", \u0107wiczenia " + (x.days || 0) + " dni (" + (x.mins || "?") + "), technika sam(a): " + (x.own || "?") + ", samopoczucie " + (x.mood || "?") + "/5" + (Array.isArray(x.rec) && x.rec.length ? "\nw tym tygodniu: " + x.rec.join(", ") : "") + (x.self ? "\ntrening z w\u0142asnej inicjatywy: " + x.self : "") + (x.field ? "\nostatnie \u0107wiczenia w terenie z rodzicem: " + x.field : "") + (x.tells ? "\nopowiada rodzicowi o Nowej Mowie: " + x.tells : "") + (Array.isArray(x.good) && x.good.length ? "\nspokojna Nowa Mowa: " + x.good.join(", ") : "") + (Array.isArray(x.hard) && x.hard.length ? "\nproblem z utrzymaniem techniki: " + x.hard.join(", ") : "") + (x.moment ? "\nmoment tygodnia: " + x.moment : "") + (x.ask ? "\npytanie do trenera: " + x.ask : "")).join("\n\n").slice(0, 6e3);
    }
  } catch (e) {
  }
  const diaryCorpus = dList.filter((x) => x.txt.length || x.med != null || x.old != null).map((x) => "### " + x.d + (x.med != null ? " \xB7 relaksacja \xD7" + x.med : "") + (x.old != null ? x.old ? " \xB7 u\u017Cywa\u0142 starej mowy" : " \xB7 bez starej mowy" : "") + "\n" + x.txt.join("\n")).join("\n\n").slice(0, 14e3);
  const corpus = recs.slice().sort((a, b) => a.d.localeCompare(b.d)).map((r) => "### " + r.d + " \xB7 " + r.cat + (r.own ? "" : " \xB7 (nagranie z konta: " + r.owner + ")") + " \xB7 " + Math.round(r.dur) + " s\n" + r.dialog.slice(0, per)).join("\n\n");
  const sys = 'Jeste\u015B asystentem trenera w programie Nowa Mowa (nauka p\u0142ynnego m\xF3wienia dla os\xF3b, kt\xF3re si\u0119 j\u0105kaj\u0105; kursanci \u0107wicz\u0105 \u201Esystemy mowy\u201D \u2014 np. zer\xF3wka, \u0142\u0105cznik, jedynka \u2014 i rozmowy w codziennych sytuacjach; harmonogram nagra\u0144; trener np. Dawid; spotkania w Miko\u0142owie). Dostajesz teksty nagra\u0144 kursanta z rozpoznawania mowy (mog\u0105 by\u0107 b\u0142\u0119dy) \u2014 wypowiedzi kursanta oznaczone \u201EKURSANT\u201D. Napisz rzeteln\u0105 analiz\u0119 dla trenera \u2014 jak do\u015Bwiadczony trener, konkretnie, z przyk\u0142adami i cytatami z tekst\xF3w. Nie wymy\u015Blaj fakt\xF3w spoza tekst\xF3w. Nigdy nie u\u017Cywaj s\u0142\xF3w \u201Eterapia\u201D, \u201Eterapeutyczny\u201D, \u201Eterapeuta\u201D \u2014 m\xF3w \u201Etrening\u201D, \u201Ekurs\u201D, \u201Eprogram\u201D. Niep\u0142ynno\u015Bci nazywaj \u201Ebloki mowy\u201D (nigdy \u201Eblokady\u201D, \u201Estaropolskie\u201D); dawny spos\xF3b m\xF3wienia to \u201Estara mowa\u201D. Daty pisz s\u0142ownie: \u201E12 pa\u017Adziernika\u201D (nigdy \u201E12 10\u201D, \u201E12.10\u201D, \u201E2026-10-12\u201D). Nie podawaj imion sprzedawc\xF3w, przechodni\xF3w ani innych obcych rozm\xF3wc\xF3w \u2014 znamy tylko ich role (chyba \u017Ce imi\u0119 wyra\u017Anie pada w tek\u015Bcie). Raport ma przede wszystkim POKAZYWA\u0106 ROZW\xD3J kursanta: por\xF3wnuj pierwsz\u0105 i drug\u0105 po\u0142ow\u0119 okresu (growth.a \u2192 growth.b) i tygodnie (weeks) \u2014 czas nagra\u0144, czas i liczb\u0119 rozm\xF3w, kategorie, samopoczucie, kontakt z innymi kursantami; podawaj konkretne liczby (np. \u201E20 nagra\u0144 jednego dnia w kategorii Przechodzie\u0144\u201D z conv[].bestDay). Nie analizuj pauz. Samopoczucie (mood) to samoocena kursanta w PitchRec po nagraniu: 1 luz, 2 komfortowo, 3 neutralnie, 4 niekomfortowo, 5 stres \u2014 po\u0142\u0105cz je z tym, co s\u0142ycha\u0107 w tekstach. Telefony: phone.trener = rozmowy z trenerem, phone.kursanci = z innymi kursantami, phone.inne = pozosta\u0142e telefony (rodzina, znajomi, urz\u0119dy, sklepy). Zadania specjalne (tasks) to wyzwania programu \u2014 opisz, jak wykonane zadania wp\u0142ywaj\u0105 na trening. Sekcja \u201Etrening\u201D odpowiada na pytanie \u201EJak te nagrania wp\u0142ywaj\u0105 na trening Nowej Mowy?\u201D \u2014 w stylu tego wzoru (dopasuj tre\u015B\u0107 do tego kursanta, cytuj przed\u0142u\u017Cenia z tekst\xF3w, podawaj liczby):\n1. Praktyczne wdra\u017Canie techniki w terenie (Automatyzacja) \u2014 w zapisie wida\u0107 stosowanie elementarnych zasad Nowej Mowy: przed\u0142u\u017Ce\u0144 / mi\u0119kkiego nastawienia g\u0142osu (np. \u201Etuuuuutaj\u201D, \u201EDzieeeeeekuuuj\u0119\u201D); u\u017Cywanie techniki w realnych sytuacjach (sklep, ulica, szko\u0142a) przenosi kontrol\u0119 z zaj\u0119\u0107 do codziennego \u017Cycia.\n2. Desensybilizacja i wygaszanie l\u0119ku spo\u0142ecznego \u2014 powtarzalne, kr\xF3tkie rozmowy z obcymi (np. 20 nagra\u0144 jednego dnia w kategorii Przechodzie\u0144) buduj\u0105 odporno\u015B\u0107 na reakcj\u0119 stresow\u0105; wielokrotna ekspozycja sprawia, \u017Ce kontakt z nieznajomym przestaje by\u0107 sytuacj\u0105 zagro\u017Cenia.\n3. Generalizacja mowy na wy\u017Cszych stopniach trudno\u015Bci \u2014 przej\u015Bcie od sytuacji bezpiecznych (rodzina, czytanie) do trudniejszych (telefon do kursanta, wyst\u0105pienia publiczne); przygotowanie do przej\u015Bcia na wy\u017Csze systemy/poziomy (szybsze tempo przy zachowaniu p\u0142ynno\u015Bci i spokoju), je\u015Bli kursant o tym m\xF3wi.\nDyskretnie traktuj sprawy zdrowotne i prywatne (bez szczeg\xF3\u0142\xF3w medycznych). Odpowiedz WY\u0141\u0104CZNIE JSON-em:\n{"rozwoj":{"wstep":"2\u20133 zdania: jak kursant si\u0119 rozwin\u0105\u0142 w tym okresie","punkty":[{"obszar":"\u2026","bylo":"\u2026","jest":"\u2026"}]},"wnioski_czas":["3 kr\xF3tkie wnioski o strukturze \u0107wicze\u0144 (na podstawie liczb)"],"tresci":[{"kategoria":"\u2026","cel":"\u2026","tematy":[{"nazwa":"\u2026","opis":"\u2026"}],"charakterystyka":"\u2026"}],"trening":{"wstep":"\u2026","wplyw":[{"tytul":"np. Praktyczne wdra\u017Canie techniki w terenie (Automatyzacja)","punkty":["\u2026","\u2026"]}],"najwazniejsze":[{"tytul":"\u2026","opis":"\u2026"}]},"emocje":{"ocena":"ocena samopoczucia i emocji z nagra\u0144 (samoocena + tekst)","zmiana":"jak si\u0119 zmienia\u0142o w czasie","wg_sytuacji":[{"kategoria":"\u2026","opis":"\u2026"}]},"zadania":[{"zadanie":"nazwa zadania specjalnego","wplyw":"jak wp\u0142ywa na trening tego kursanta"}],"kontakty":{"opis":"kontakt z innymi kursantami i z trenerem \u2014 znaczenie dla treningu","punkty":["\u2026"]},"rozmowy":["wnioski o czasie i liczbie rozm\xF3w (d\u0142u\u017Csze rozmowy, wi\u0119cej wymian, trudniejsze sytuacje)"],"slownictwo":{"wg_sytuacji":[{"kategoria":"\u2026","opis":"\u2026"}],"technika":["przejawy techniki mowy w zapisie (przed\u0142u\u017Cenia, mi\u0119kki start, kotwice) z przyk\u0142adami"],"matryce":["typowe pytania i formu\u0142y z przyk\u0142adami"],"kregi":[{"nazwa":"\u2026","slowa":"\u2026"}],"skladnia":["\u2026"]},"pewnosc":{"osiagniecia":[{"tytul":"\u2026","osiagniecie":"\u2026","wymiar":"\u2026"}],"trudnosci":[{"rodzaj":"\u2026","bariera":"\u2026","sposob":"\u2026"}],"ewolucja":[{"tytul":"\u2026","opis":"\u2026"}],"podsumowanie":"\u2026"},"rodzic":{"podsumowanie":"2\u20134 zdania: co wynika z cotygodniowych obserwacji rodzica (tylko je\u015Bli s\u0105 dane)","zmiany":["zmiany tydzie\u0144 po tygodniu (mowa, \u0107wiczenia, samopoczucie) z liczbami"],"wskazowki":["co rodzic mo\u017Ce robi\u0107 w domu, \u017Ceby wspiera\u0107 trening"]},"dziennik":{"podsumowanie":"2\u20134 zdania: co wynika z dziennika (regularno\u015B\u0107, samopoczucie, relaksacja, stara mowa, sen, motywacja)","najwazniejsze":["najwa\u017Cniejsze obserwacje z wpis\xF3w, z kr\xF3tkimi cytatami"],"wplyw":[{"tytul":"\u2026","opis":"jak to wp\u0142ywa na trening Nowej Mowy i co z tym zrobi\u0107"}],"sygnaly":["sygna\u0142y ostrzegawcze (spadek motywacji, powr\xF3t starej mowy, brak relaksacji, zm\u0119czenie) \u2014 tylko je\u015Bli s\u0105 w danych"]},"etapy":[{"okres":"\u2026","opis":"\u2026"}],"dalej":["co trener mo\u017Ce zrobi\u0107 dalej z tym kursantem \u2014 konkretnie"]}';
  const statsBrief = JSON.stringify({ ...stats, otherRecs: void 0, tasks: tasks.map((x) => ({ zadanie: x.code + " " + x.title, opis: x.desc, notatka: x.note, wykonane: x.done, w_okresie: x.inPeriod })) });
  let ai = null;
  try {
    ai = txParseJson(await txLlm(
      env,
      sys + " Dostajesz te\u017C DZIENNIK kursanta (codzienne wpisy: relaksacja, stara mowa, samopoczucie, trudne i \u0142atwe sytuacje, motywacja, sen) \u2014 przeanalizuj go w polu \u201Edziennik\u201D: wy\u0142uskaj to, co najwa\u017Cniejsze i ma wp\u0142yw na trening Nowej Mowy; \u0142\u0105cz go z nagraniami (np. gorsze samopoczucie \u2194 mniej nagra\u0144). stats.diary to liczby z dziennika.",
      "LICZBY (wyliczone dok\u0142adnie):\n" + statsBrief + (parentCorpus ? "\n\nOBSERWACJE RODZICA (co tydzie\u0144, ankieta rodzica; stats.parent to liczby):\n" + parentCorpus : "") + "\n\nDZIENNIK KURSANTA (" + diary.days + " dni z wpisem na " + days + "):\n" + (diaryCorpus || "(brak wpis\xF3w w tym okresie)") + "\n\nTEKSTY NAGRA\u0143:\n" + corpus,
      1e4
    ));
  } catch (e) {
  }
  if (ai) {
    try {
      ai = srFixDeep(ai);
    } catch (e) {
    }
  }
  if (questions.length) {
    try {
      const qa = txParseJson(await txLlm(
        env,
        'Dostajesz pytania wyci\u0105gni\u0119te automatycznie z rozpoznawania mowy (polski, mog\u0105 by\u0107 b\u0142\u0119dy) z nagra\u0144 kursanta w codziennych sytuacjach, pogrupowane wg kategorii nagrania; n = ile razy pad\u0142o. Popraw b\u0142\u0119dy rozpoznawania tak, by pytanie brzmia\u0142o naturalnie i pasowa\u0142o do sytuacji (np. \u201EUrz\u0119dy do Hali Wola?\u201D \u2192 \u201EKt\xF3r\u0119dy do Hali Wola?\u201D). Po\u0142\u0105cz pytania o tym samym sensie (zsumuj n). Usu\u0144 zwroty bez sensu, urwane, nieb\u0119d\u0105ce pytaniami do rozm\xF3wcy, wtr\u0105cenia do trenera lub nagrywaj\u0105cego (np. \u201EMo\u017Cna usun\u0105\u0107 blanem skeczu?\u201D, \u201EWiesz co, Dawid?\u201D). Niczego nie wymy\u015Blaj ani nie dodawaj. Odpowiedz WY\u0141\u0104CZNIE JSON-em: {"pytania":[{"cat":"kategoria dok\u0142adnie jak na wej\u015Bciu","top":[{"q":"\u2026","n":1}]}]}',
        JSON.stringify(questions.map((c) => ({ cat: c.cat, top: c.top }))),
        2500
      ));
      const P = qa && Array.isArray(qa.pytania) ? qa.pytania : null;
      if (P) {
        const QT = /^(przepraszam[, ]+)?(czy|gdzie|ile|jak|jaki|jaka|jakie|który|która|które|którędy|co|kiedy|dlaczego|skąd|dokąd|kto|o której|można|mogę|macie)\b/i;
        const out = [];
        for (const c of questions) {
          const a = P.find((x) => x && String(x.cat) === String(c.cat));
          if (!a || !Array.isArray(a.top)) {
            out.push(c);
            continue;
          }
          const top = a.top.map((x) => ({ q: String(x && x.q || "").trim().slice(0, 160), n: Math.max(1, Number(x && x.n) || 1) })).filter((x) => x.q.length >= 4).slice(0, 10);
          if (!top.length) continue;
          const t = {};
          top.forEach((x) => {
            const w = (x.q.toLowerCase().match(QT) || [])[2];
            if (w) t[w] = (t[w] || 0) + x.n;
          });
          const n = top.reduce((s, x) => s + x.n, 0);
          if (n < 2) continue;
          out.push({ cat: c.cat, n, ai: true, types: Object.keys(t).sort((x, y) => t[y] - t[x]).map((k) => ({ w: k, n: t[k] })), top: top.sort((x, y) => y.n - x.n) });
        }
        stats.questions = out.sort((x, y) => y.n - x.n);
      }
    } catch (e) {
    }
  }
  try {
    const K = ai && ai.slownictwo && Array.isArray(ai.slownictwo.kregi) ? ai.slownictwo.kregi : null;
    if (K && K.length) {
      const fold = /* @__PURE__ */ __name((t) => String(t).toLowerCase().normalize("NFD").replace(/[\u0300-\u036f]/g, "").replace(/ł/g, "l"), "fold");
      const low = " " + fold(recs.map((r) => String(r.kText || "")).join(" \n ")).replace(/[„”"«»]/g, " ") + " ";
      const cnt = /* @__PURE__ */ __name((ph) => {
        const p = fold(ph).replace(/[…]|\.\.\./g, " ").replace(/[.,!?;:()]+/g, " ").replace(/\s+/g, " ").trim();
        if (p.length < 2) return 0;
        const rx = new RegExp("(^|[^\\p{L}])" + p.replace(/[.*+?^${}()|[\]\\]/g, "\\$&").replace(/ /g, "[\\s,.!?;:]+") + "(?=[^\\p{L}]|$)", "giu");
        return (low.match(rx) || []).length;
      }, "cnt");
      let all = 0;
      ai.slownictwo.kregi_licznik = K.map((k) => {
        const nazwa = typeof k === "string" ? k : String(k.nazwa || k.krag || "");
        const sl = typeof k === "string" ? "" : String(k.slowa || "");
        const items = sl.split(/[,;\n]+/).map((x) => x.replace(/[…]|\.\.\./g, "").trim().replace(/[.!?]+$/, "")).filter((x) => x.length >= 2).slice(0, 25).map((x) => ({ s: x, n: cnt(x) }));
        const razem = items.reduce((t, x) => t + x.n, 0);
        all += razem;
        return { nazwa, slowa: items, razem };
      });
      ai.slownictwo.kregi_razem = all;
    }
  } catch (e) {
  }
  const report = { stats, ai: ai || null, ai_error: ai ? null : "AI nie zwr\xF3ci\u0142o opisu \u2014 spr\xF3buj ponownie", created_at: Math.floor(Date.now() / 1e3), version: 2 };
  await env.DB.prepare("INSERT INTO student_report (ns_student_id, period, report, created_at) VALUES (?, ?, ?, ?) ON CONFLICT(ns_student_id, period) DO UPDATE SET report = excluded.report, created_at = excluded.created_at").bind(String(sid), String(days), JSON.stringify(report), report.created_at).run();
  return { ok: true, report };
}
__name(txStudentReport, "txStudentReport");
__name2(txStudentReport, "txStudentReport");
async function srJobTable(env) {
  try {
    await env.DB.prepare("CREATE TABLE IF NOT EXISTS student_report_job (ns_student_id TEXT PRIMARY KEY, days INTEGER, status TEXT, error TEXT, requested_by TEXT, created_at INTEGER, started_at INTEGER, finished_at INTEGER, tries INTEGER DEFAULT 0, extra TEXT)").run();
  } catch (e) {
  }
  try {
    await env.DB.prepare("ALTER TABLE student_report_job ADD COLUMN extra TEXT").run();
  } catch (e) {
  }
}
__name(srJobTable, "srJobTable");
__name2(srJobTable, "srJobTable");
async function srJobTick(env) {
  if (!env.HYPERDRIVE) return;
  await srJobTable(env);
  const now = Math.floor(Date.now() / 1e3);
  await env.DB.prepare("UPDATE student_report_job SET status = 'error', error = 'Raport nie powsta\u0142 (przekroczony czas) \u2014 spr\xF3buj ponownie', finished_at = ? WHERE status = 'running' AND started_at < ? AND tries >= 2").bind(now, now - 720).run();
  const job = await env.DB.prepare("SELECT * FROM student_report_job WHERE status = 'queued' OR (status = 'running' AND started_at < ? AND tries < 2) ORDER BY created_at LIMIT 1").bind(now - 720).first();
  if (!job) return;
  const claim = await env.DB.prepare("UPDATE student_report_job SET status = 'running', started_at = ?, tries = tries + 1 WHERE ns_student_id = ? AND created_at = ? AND status = ?").bind(now, job.ns_student_id, job.created_at, job.status).run();
  if (!claim.meta || !claim.meta.changes) return;
  let r;
  try {
    await ensureTranscriptTables(env);
    let ex = null;
    try {
      ex = job.extra ? JSON.parse(job.extra) : null;
    } catch (e) {
    }
    r = await txStudentReport(env, job.ns_student_id, job.days, ex);
  } catch (e) {
    r = { ok: false, error: String(e && e.message || e).slice(0, 300) };
  }
  await env.DB.prepare("UPDATE student_report_job SET status = ?, error = ?, finished_at = ? WHERE ns_student_id = ? AND created_at = ?").bind(r.ok ? "done" : "error", r.ok ? r.report && r.report.ai_error || null : r.error || "B\u0142\u0105d", Math.floor(Date.now() / 1e3), job.ns_student_id, job.created_at).run();
}
__name(srJobTick, "srJobTick");
__name2(srJobTick, "srJobTick");
var RS_MOOD = { luz: 1, komfortowo: 2, neutralnie: 3, niekomfortowo: 4, stres: 5 };
async function rsSalt(env) {
  if (env.RESEARCH_SALT) return String(env.RESEARCH_SALT);
  await ensureTranscriptTables(env);
  const r = await env.DB.prepare("SELECT v FROM tx_kv WHERE k = 'research_salt'").first();
  if (r && r.v) return r.v;
  const b = new Uint8Array(24);
  crypto.getRandomValues(b);
  const v = [...b].map((x) => x.toString(16).padStart(2, "0")).join("");
  await env.DB.prepare("INSERT INTO tx_kv (k, v) VALUES ('research_salt', ?) ON CONFLICT(k) DO NOTHING").bind(v).run();
  const again = await env.DB.prepare("SELECT v FROM tx_kv WHERE k = 'research_salt'").first();
  return again && again.v || v;
}
__name(rsSalt, "rsSalt");
__name2(rsSalt, "rsSalt");
async function rsPseudo(salt) {
  const key = await crypto.subtle.importKey("raw", new TextEncoder().encode(salt), { name: "HMAC", hash: "SHA-256" }, false, ["sign"]);
  const cache = {};
  return async (prefix, id) => {
    const k = prefix + ":" + id;
    if (cache[k]) return cache[k];
    const sig = new Uint8Array(await crypto.subtle.sign("HMAC", key, new TextEncoder().encode(k)));
    return cache[k] = prefix + "-" + [...sig.slice(0, 5)].map((x) => x.toString(16).padStart(2, "0")).join("");
  };
}
__name(rsPseudo, "rsPseudo");
__name2(rsPseudo, "rsPseudo");
function rsCsv(rows, cols) {
  const esc = /* @__PURE__ */ __name((v) => {
    if (v === null || v === void 0) return "";
    const s = String(v);
    return /[",;\n\r]/.test(s) ? '"' + s.replace(/"/g, '""') + '"' : s;
  }, "esc");
  return "\uFEFF" + cols.join(",") + "\n" + rows.map((r) => cols.map((c) => esc(r[c])).join(",")).join("\n") + "\n";
}
__name(rsCsv, "rsCsv");
__name2(rsCsv, "rsCsv");
function rsFile(file) {
  const f = String(file || "").split("/").pop();
  const core = f.replace(/\.(mp3|m4a|wav|ogg|webm|aac)$/i, "").replace(/_(GPS|teren|dom)[\s\S]*$/i, "");
  const m = core.match(/_-([A-Za-z][A-Za-z0-9]*)-([a-zA-ZąćęłńóśźżĄĆĘŁŃÓŚŹŻ]+)$/);
  const tm = f.match(/_(\d{1,2})h-(\d{1,2})m-(\d{1,2})s_/), ter = f.match(/teren-(\d+(?:\.\d+)?)km/i);
  return {
    system: m ? m[1] : null,
    mood: m ? m[2].toLowerCase() : null,
    hour: tm ? Number(tm[1]) : null,
    teren_km: ter ? Number(ter[1]) : null,
    has_gps: /_GPS-?\d/.test(f),
    dom: /_dom(\.|_|$)/i.test(f)
  };
}
__name(rsFile, "rsFile");
__name2(rsFile, "rsFile");
var RS_WD = ["nd", "pn", "wt", "sr", "cz", "pt", "so"];
function rsWarsaw(d) {
  if (!d) return { day: null, hour: null };
  const p = new Intl.DateTimeFormat("sv-SE", { timeZone: "Europe/Warsaw", year: "numeric", month: "2-digit", day: "2-digit", hour: "2-digit", hourCycle: "h23" }).formatToParts(d instanceof Date ? d : new Date(d));
  const g = /* @__PURE__ */ __name((t) => (p.find((x) => x.type === t) || {}).value, "g");
  return { day: g("year") + "-" + g("month") + "-" + g("day"), hour: Number(g("hour")) };
}
__name(rsWarsaw, "rsWarsaw");
__name2(rsWarsaw, "rsWarsaw");
async function rsInventory(env) {
  await ensureTranscriptTables(env);
  const pg = new Client({ connectionString: env.HYPERDRIVE.connectionString });
  await pg.connect();
  const out = {};
  try {
    const q = /* @__PURE__ */ __name(async (sql, p) => (await pg.query(sql, p || [])).rows, "q");
    out.records = (await q(`SELECT COUNT(*)::int AS n, COUNT(DISTINCT user_id)::int AS students, MIN(COALESCE(date, created_at::date))::text AS first, MAX(COALESCE(date, created_at::date))::text AS last,
        COUNT(*) FILTER (WHERE reviewed_at IS NOT NULL)::int AS reviewed, COUNT(*) FILTER (WHERE is_correct = true)::int AS correct,
        COUNT(*) FILTER (WHERE review_details LIKE '\u{1F6AB}%')::int AS not_counted,
        COUNT(*) FILTER (WHERE record_file ~ '_-[A-Za-z][A-Za-z0-9]*-(luz|komfortowo|neutralnie|niekomfortowo|stres)')::int AS with_mood,
        COUNT(*) FILTER (WHERE record_file LIKE '%\\_GPS%')::int AS with_gps, COUNT(*) FILTER (WHERE record_file LIKE '%teren-%')::int AS with_teren
      FROM records WHERE deleted_at IS NULL AND record_file IS NOT NULL`))[0];
    out.by_year = await q(`SELECT EXTRACT(YEAR FROM COALESCE(date, created_at::date))::int AS year, COUNT(*)::int AS n, COUNT(DISTINCT user_id)::int AS students
      FROM records WHERE deleted_at IS NULL AND record_file IS NOT NULL GROUP BY 1 ORDER BY 1`);
    out.by_country = await q(`SELECT COALESCE(u.country, '?') AS country, COUNT(DISTINCT r.user_id)::int AS students, COUNT(*)::int AS n
      FROM records r JOIN users u ON u.id = r.user_id WHERE r.deleted_at IS NULL AND r.record_file IS NOT NULL GROUP BY 1 ORDER BY 3 DESC`);
    out.by_category = await q(`SELECT COALESCE(rct.name, rc.name) AS category, COUNT(*)::int AS n, COUNT(DISTINCT r.user_id)::int AS students
      FROM records r LEFT JOIN record_categories rc ON rc.id = r.record_category_id
      LEFT JOIN record_category_translations rct ON rct.record_category_id = rc.id AND rct.locale = 'pl'
      WHERE r.deleted_at IS NULL AND r.record_file IS NOT NULL GROUP BY 1 ORDER BY 2 DESC LIMIT 40`);
    out.per_student = (await q(`SELECT percentile_cont(0.5) WITHIN GROUP (ORDER BY n)::int AS median, MAX(n)::int AS max,
        COUNT(*) FILTER (WHERE n >= 50)::int AS ge50, COUNT(*) FILTER (WHERE n >= 200)::int AS ge200, COUNT(*) FILTER (WHERE months >= 3)::int AS months3plus
      FROM (SELECT user_id, COUNT(*) AS n, (MAX(COALESCE(date, created_at::date)) - MIN(COALESCE(date, created_at::date))) / 30 AS months FROM records
            WHERE deleted_at IS NULL AND record_file IS NOT NULL GROUP BY user_id) x`))[0];
    try {
      out.rates = (await q(`SELECT COUNT(*)::int AS n, COUNT(DISTINCT record_id)::int AS records FROM record_rates`))[0];
    } catch (e) {
      out.rates = null;
    }
    try {
      out.diaries = (await q(`SELECT COUNT(*)::int AS n, COUNT(DISTINCT user_id)::int AS students FROM diaries WHERE deleted_at IS NULL`))[0];
    } catch (e) {
      out.diaries = null;
    }
    try {
      out.visits = (await q(`SELECT COUNT(*)::int AS n, COUNT(DISTINCT student_id)::int AS students FROM visit_dates`))[0];
    } catch (e) {
      out.visits = null;
    }
  } finally {
    try {
      await pg.end();
    } catch (e) {
    }
  }
  const d1 = /* @__PURE__ */ __name(async (sql) => {
    try {
      return await env.DB.prepare(sql).first();
    } catch (e) {
      return null;
    }
  }, "d1");
  out.transcripts = await d1(`SELECT COUNT(*) AS n, ROUND(SUM(dur) / 3600.0, 1) AS hours, SUM(CASE WHEN model LIKE 'android-asr%' THEN 1 ELSE 0 END) AS phone,
      SUM(CASE WHEN speakers IS NOT NULL THEN 1 ELSE 0 END) AS split, SUM(CASE WHEN fixed IS NOT NULL THEN 1 ELSE 0 END) AS fixed,
      SUM(CASE WHEN spk_people IS NOT NULL THEN 1 ELSE 0 END) AS people, SUM(CASE WHEN edited_by IS NOT NULL THEN 1 ELSE 0 END) AS edited
    FROM record_transcript WHERE status = 'ok'`);
  out.special_tasks = await d1(`SELECT COUNT(*) AS n, COUNT(DISTINCT ns_student_id) AS students FROM special_task_done`);
  out.disc = await d1(`SELECT COUNT(*) AS n, COUNT(DISTINCT ns_user_id) AS students FROM disc_results`);
  out.call_days = await d1(`SELECT COUNT(*) AS n, COUNT(DISTINCT ns_student_id) AS students FROM call_days`);
  out.review_log = await d1(`SELECT COUNT(*) AS n, COUNT(DISTINCT trainer_email) AS trainers FROM review_log`);
  out.system_override = await d1(`SELECT COUNT(*) AS n FROM record_system_override`);
  return { ok: true, inventory: out, generated_at: (/* @__PURE__ */ new Date()).toISOString() };
}
__name(rsInventory, "rsInventory");
__name2(rsInventory, "rsInventory");
async function rsExport(env, url) {
  const kind = url.searchParams.get("kind") || "recordings";
  const P = await rsPseudo(await rsSalt(env));
  await ensureTranscriptTables(env);
  const pg = new Client({ connectionString: env.HYPERDRIVE.connectionString });
  await pg.connect();
  try {
    if (kind === "students") {
      const rows2 = (await pg.query(`SELECT u.id::text AS id, u.country, u.created_at, to_jsonb(u) ->> 'gender' AS gender, to_jsonb(u) ->> 'birth_date' AS birth_date,
          COUNT(r.id)::int AS n, MIN(COALESCE(r.date, r.created_at::date))::text AS first, MAX(COALESCE(r.date, r.created_at::date))::text AS last,
          COUNT(r.id) FILTER (WHERE r.is_correct = true)::int AS correct, COUNT(r.id) FILTER (WHERE r.reviewed_at IS NOT NULL)::int AS reviewed
        FROM users u JOIN records r ON r.user_id = u.id AND r.deleted_at IS NULL AND r.record_file IS NOT NULL
        GROUP BY u.id ORDER BY MIN(r.created_at)`)).rows;
      let disc = {};
      try {
        for (const x of (await env.DB.prepare("SELECT ns_user_id, profile_code FROM disc_results ORDER BY created_at").all()).results || []) disc[x.ns_user_id] = x.profile_code;
      } catch (e) {
      }
      let tasks = {};
      try {
        for (const x of (await env.DB.prepare("SELECT ns_student_id, COUNT(*) AS n FROM special_task_done GROUP BY ns_student_id").all()).results || []) tasks[x.ns_student_id] = x.n;
      } catch (e) {
      }
      const yr = /* @__PURE__ */ __name((d) => {
        const y = d ? Number(String(d).slice(0, 4)) : null;
        if (!y) return null;
        const a = (/* @__PURE__ */ new Date()).getUTCFullYear() - y;
        return a < 18 ? "<18" : a < 30 ? "18-29" : a < 45 ? "30-44" : a < 60 ? "45-59" : "60+";
      }, "yr");
      const out2 = [];
      for (const r of rows2) out2.push({
        pid: await P("K", r.id),
        country: r.country || "",
        start_month: r.created_at ? new Date(r.created_at).toISOString().slice(0, 7) : "",
        gender: r.gender || "",
        age_band: yr(r.birth_date) || "",
        recordings: r.n,
        reviewed: r.reviewed,
        correct: r.correct,
        first_record: r.first,
        last_record: r.last,
        disc_profile: disc[r.id] || "",
        special_tasks_done: tasks[r.id] || 0
      });
      return { csv: rsCsv(out2, ["pid", "country", "start_month", "gender", "age_band", "recordings", "reviewed", "correct", "first_record", "last_record", "disc_profile", "special_tasks_done"]), n: out2.length };
    }
    if (kind === "diaries") {
      const rows2 = (await pg.query(`SELECT d.user_id::text AS uid, d.date, d.is_correct, to_jsonb(d) AS j FROM diaries d WHERE d.deleted_at IS NULL ORDER BY d.date`)).rows;
      const out2 = [];
      for (const r of rows2) {
        const j = r.j || {};
        out2.push({
          pid: await P("K", r.uid),
          date: r.date ? rsWarsaw(r.date).day : "",
          is_correct: r.is_correct === true ? 1 : r.is_correct === false ? 0 : "",
          meditation_count: j.meditation_count != null ? j.meditation_count : "",
          old_speech_usage: j.old_speech_usage === true ? 1 : j.old_speech_usage === false ? 0 : "",
          talk_feeling_id: j.talk_feeling_id || ""
        });
      }
      return { csv: rsCsv(out2, ["pid", "date", "is_correct", "meditation_count", "old_speech_usage", "talk_feeling_id"]), n: out2.length };
    }
    const month = /^\d{4}-\d{2}$/.test(url.searchParams.get("month") || "") ? url.searchParams.get("month") : (/* @__PURE__ */ new Date()).toISOString().slice(0, 7);
    const from = month + "-01", toD = /* @__PURE__ */ new Date(from + "T12:00:00Z");
    toD.setUTCMonth(toD.getUTCMonth() + 1);
    const to = toD.toISOString().slice(0, 10);
    const rows = (await pg.query(`SELECT r.id::text AS id, r.user_id::text AS uid, COALESCE(r.date, r.created_at::date)::text AS d, r.created_at, r.record_file, r.is_correct, r.reviewed_at,
        r.review_details LIKE '\u{1F6AB}%' AS not_counted, COALESCE(rct.name, rc.name) AS category, u.country, u.created_at AS u_created,
        (SELECT string_agg(COALESCE(rrt.name, '?') || ':' || COALESCE(ru.percent_mark::text, ''), ';') FROM record_rates rr
           LEFT JOIN record_rate_category_translations rrt ON rrt.record_rate_category_id = rr.record_rate_category_id AND rrt.locale = 'pl'
           LEFT JOIN record_rate_units ru ON ru.id = rr.record_rate_unit_id WHERE rr.record_id = r.id) AS rates,
        (SELECT COUNT(*) FROM records p WHERE p.user_id = r.user_id AND p.deleted_at IS NULL AND p.record_file IS NOT NULL AND p.created_at < r.created_at)::int AS idx_all,
        (SELECT COUNT(*) FROM records p WHERE p.user_id = r.user_id AND p.record_category_id = r.record_category_id AND p.deleted_at IS NULL AND p.record_file IS NOT NULL AND p.created_at < r.created_at)::int AS idx_cat
      FROM records r JOIN users u ON u.id = r.user_id
      LEFT JOIN record_categories rc ON rc.id = r.record_category_id
      LEFT JOIN record_category_translations rct ON rct.record_category_id = rc.id AND rct.locale = 'pl'
      WHERE r.deleted_at IS NULL AND r.record_file IS NOT NULL AND COALESCE(r.date, r.created_at::date) >= $1::date AND COALESCE(r.date, r.created_at::date) < $2::date
      ORDER BY r.created_at`, [from, to])).rows;
    try {
      await pg.end();
    } catch (e) {
    }
    const tx = {}, ov = {};
    for (let i = 0; i < rows.length; i += 90) {
      const ch = rows.slice(i, i + 90).map((r) => r.id);
      const rs = await env.DB.prepare(`SELECT ns_record_id, dur, words, speakers, spk_names, spk_src, model FROM record_transcript WHERE status = 'ok' AND ns_record_id IN (${ch.map(() => "?").join(",")})`).bind(...ch).all();
      for (const x of rs.results || []) tx[x.ns_record_id] = x;
      try {
        const o = await env.DB.prepare(`SELECT ns_record_id, system FROM record_system_override WHERE ns_record_id IN (${ch.map(() => "?").join(",")})`).bind(...ch).all();
        for (const x of o.results || []) ov[x.ns_record_id] = x.system;
      } catch (e) {
      }
    }
    const out = [];
    for (const r of rows) {
      const f = rsFile(r.record_file), w = rsWarsaw(r.created_at), t = tx[r.id];
      const o = {
        pid: await P("K", r.uid),
        rid: await P("R", r.id),
        date: r.d,
        weekday: RS_WD[(/* @__PURE__ */ new Date(r.d + "T12:00:00Z")).getUTCDay()],
        hour: f.hour !== null ? f.hour : w.hour,
        days_since_start: r.u_created ? Math.floor((Date.parse(r.d + "T12:00:00Z") - new Date(r.u_created).getTime()) / 864e5) : "",
        record_no: r.idx_all + 1,
        record_no_in_category: r.idx_cat + 1,
        country: r.country || "",
        category: r.category || "",
        system: ov[r.id] || f.system || "",
        system_by_trainer: ov[r.id] ? 1 : 0,
        mood: f.mood || "",
        mood_level: f.mood ? RS_MOOD[f.mood] || "" : "",
        teren_km: f.teren_km !== null ? f.teren_km : "",
        has_gps: f.has_gps ? 1 : 0,
        at_home: f.dom ? 1 : 0,
        reviewed: r.reviewed_at ? 1 : 0,
        is_correct: r.is_correct === true ? 1 : r.is_correct === false ? 0 : "",
        not_counted: r.not_counted ? 1 : 0,
        rates: r.rates || ""
      };
      if (t) {
        let words = [], sp = null, names = {};
        try {
          words = JSON.parse(t.words || "[]");
        } catch (e) {
        }
        try {
          sp = t.speakers ? JSON.parse(t.speakers) : null;
        } catch (e) {
        }
        try {
          names = t.spk_names ? JSON.parse(t.spk_names) || {} : {};
        } catch (e) {
        }
        const spOk = sp && sp.length === words.length, persons = spOk ? new Set(sp).size : 1;
        let kIdx = null;
        if (spOk && persons > 1) {
          for (const k of Object.keys(names)) if (/kursant/i.test(String(names[k]))) {
            kIdx = Number(k);
            break;
          }
          if (kIdx === null && t.spk_src === "ai-text") kIdx = 0;
        }
        const m = persons < 2 ? srMeasure(words, t.dur, 20) : null;
        Object.assign(o, {
          dur_s: Math.round((Number(t.dur) || 0) * 10) / 10,
          text_source: /^android-asr/.test(String(t.model || "")) ? "phone" : "whisper",
          words: words.length,
          speakers: persons,
          student_words: persons < 2 ? words.length : kIdx !== null ? sp.filter((x) => x === kIdx).length : "",
          turns: spOk ? sp.reduce((a, x, i) => a + (i && x !== sp[i - 1] ? 1 : 0), 0) : 0,
          syl_min: m ? m.syl_min : "",
          syl_min_speech: m ? m.syl_min_speech : "",
          pause_pct: m ? m.pause_pct : "",
          pauses_1s: m ? m.pauses_1s : ""
        });
      }
      out.push(o);
    }
    const cols = [
      "pid",
      "rid",
      "date",
      "weekday",
      "hour",
      "days_since_start",
      "record_no",
      "record_no_in_category",
      "country",
      "category",
      "system",
      "system_by_trainer",
      "mood",
      "mood_level",
      "teren_km",
      "has_gps",
      "at_home",
      "reviewed",
      "is_correct",
      "not_counted",
      "rates",
      "dur_s",
      "text_source",
      "words",
      "speakers",
      "student_words",
      "turns",
      "syl_min",
      "syl_min_speech",
      "pause_pct",
      "pauses_1s"
    ];
    return { csv: rsCsv(out, cols), n: out.length, month };
  } finally {
    try {
      await pg.end();
    } catch (e) {
    }
  }
}
__name(rsExport, "rsExport");
__name2(rsExport, "rsExport");
var RS_DOC_URL = "https://claude.ai/artifact/2jHj5o4mXQRFPzQrqUZo2J";
var RS_NOTES_SEED = [
  { slug: "dokument", sort: 10, title: "\u{1F4C4} Dokument dla Politechniki \u015Al\u0105skiej", body: `Pe\u0142ny opis danych, propozycji bada\u0144, sprawdzania automatyki, ogranicze\u0144 i etyki: [Nowa Mowa \u2014 dane do bada\u0144 z Politechnik\u0105 \u015Al\u0105sk\u0105](${RS_DOC_URL})

- Dokument jest prywatny. Przed wys\u0142aniem: otw\xF3rz go \u2192 **Share** \u2192 dodaj osob\u0119 z Politechniki (ogl\u0105danie, komentowanie albo edycja).
- Gdy w Share nie da si\u0119 doda\u0107 osoby z zewn\u0105trz: kliknij nazw\u0119 dokumentu \u2192 **Export** \u2192 PDF albo Word i wy\u015Blij plik.
- Plik\xF3w CSV z danymi nie wysy\u0142amy przed podpisaniem umowy i zebraniem zg\xF3d kursant\xF3w.` },
  { slug: "dane", sort: 20, title: "\u{1F5C2} Jakie dane mamy", body: `Liczby (nagrania, kursanci, godziny, lata, kraje, sytuacje): przycisk **\u{1F4CA} Poka\u017C, ile danych mamy** powy\u017Cej.

| \u0179r\xF3d\u0142o | Co zawiera |
| --- | --- |
| Nagrania (PitchRec) | d\u017Awi\u0119k, data i godzina, sytuacja, system mowy, samoocena po nagraniu (1 luz \u2013 5 stres), odleg\u0142o\u015B\u0107 od domu |
| Oceny trenera | poprawne / do poprawy, kryteria z procentem, komentarz, nagrania niewliczone, system poprawiony przez trenera |
| Teksty nagra\u0144 | tekst z czasem ka\u017Cdego s\u0142owa (Whisper albo telefon), podzia\u0142 rozmowy na osoby |
| Miary mowy | tempo w sylabach na minut\u0119, udzia\u0142 ciszy, pauzy \u2265 1 s, s\u0142owa kursanta, zmiany osoby m\xF3wi\u0105cej |
| Dzienniki | ocena dnia, medytacje, u\u017Cycie \u201Estarej mowy\u201D, samopoczucie w rozmowach |
| Harmonogramy | okresy kursu, wymagane i wykonane nagrania, system mowy w okresie |
| Kontakty | telefony mi\u0119dzy kursantami i do trenera, dni telefonu do trenera |

Najwi\u0119ksza warto\u015B\u0107: sytuacja z \u017Cycia + samoocena stresu tu\u017C po rozmowie + nagranie d\u017Awi\u0119ku, powtarzane u tej samej osoby przez miesi\u0105ce.` },
  { slug: "badania", sort: 30, title: "\u{1F9EA} Proponowane badania", body: `| Nr | Temat | Pytanie | Dane |
| --- | --- | --- | --- |
| 1 | Oswajanie l\u0119ku w terenie | Czy samoocena stresu spada z kolejnymi powt\xF3rzeniami tej samej sytuacji i jak szybko? | samoocena, numer nagrania w sytuacji, sytuacja (CSV) |
| 2 | Tempo mowy na kolejnych systemach | Jak zmienia si\u0119 tempo i pauzy od Basic do Full i czy ocena trenera si\u0119 utrzymuje? | tempo, pauzy, system, ocena (CSV) |
| 3 | Wyj\u015Bcie poza stref\u0119 komfortu | Czy kursanci nagrywaj\u0105 coraz dalej od domu i w trudniejszych sytuacjach? | odleg\u0142o\u015B\u0107, sytuacja, godzina, samoocena (CSV) |
| 4 | Cechy g\u0142osu a samoocena stresu | Czy wysoko\u015B\u0107 i zmienno\u015B\u0107 g\u0142osu, g\u0142o\u015Bno\u015B\u0107, pauzy przewiduj\u0105 stres i ocen\u0119 trenera? | pliki audio + CSV |
| 5 | Wykrywanie niep\u0142ynno\u015Bci w nagraniach z \u017Cycia | Czy automat wykryje przed\u0142u\u017Cenia, blokady i powt\xF3rzenia w ha\u0142asie i przez telefon? | pliki audio + r\u0119cznie oznaczona pr\xF3bka |
| 6 | Rozpoznawanie mowy u os\xF3b, kt\xF3re si\u0119 j\u0105kaj\u0105 | Jak bardzo Whisper i telefon wyg\u0142adzaj\u0105 niep\u0142ynno\u015Bci? Jak dok\u0142adny jest podzia\u0142 na osoby? | teksty + r\u0119czny zapis pr\xF3bki |
| 7 | Ile \u0107wicze\u0144 daje efekt | Czy minuty, regularno\u015B\u0107 i harmonogram wi\u0105\u017C\u0105 si\u0119 z popraw\u0105 oceny i samopoczucia? | wszystkie CSV |

Od razu z CSV: tematy 1, 2, 3, 6 i 7. Tematy 4 i 5 (najbli\u017Csze in\u017Cynierii biomedycznej) wymagaj\u0105 przekazania plik\xF3w d\u017Awi\u0119kowych.` },
  { slug: "automatyka", sort: 40, title: "\u2705 Jak sprawdzamy automatyk\u0119", body: `Nie ods\u0142uchujemy wszystkich nagra\u0144. Automat sprawdzamy na losowej pr\xF3bce oznaczonej przez ludzi, potem stosujemy go do ca\u0142ego zbioru.

| Co robi automat | Wzorzec | Pr\xF3bka | Miara |
| --- | --- | --- | --- |
| Tekst nagrania (Whisper, telefon) | zapis dos\u0142owny z niep\u0142ynno\u015Bciami, r\u0119czny | ok. 100 nagra\u0144 | WER, ile niep\u0142ynno\u015Bci zgin\u0119\u0142o |
| Podzia\u0142 rozmowy na osoby | r\u0119czne zaznaczenie, kto m\xF3wi | ok. 50 rozm\xF3w | odsetek s\u0142\xF3w przypisanych w\u0142a\u015Bciwej osobie |
| Tempo i pauzy | pomiar w Praacie | ok. 50 monolog\xF3w | korelacja, \u015Bredni b\u0142\u0105d |
| Cechy g\u0142osu (openSMILE / Praat) | uznane narz\u0119dzia | wszystkie | odrzucenie nagra\u0144 z przesterem, szumem, za cichych |
| Wykrywanie niep\u0142ynno\u015Bci (do zbudowania) | dwie osoby niezale\u017Cnie oznaczaj\u0105 | 300\u2013500 fragment\xF3w po kilka sekund | kappa, czu\u0142o\u015B\u0107, precyzja |
| Ocena trenera | dw\xF3ch trener\xF3w ocenia te same nagrania | ok. 5% nagra\u0144, losowo | kappa Cohena |

Pr\xF3bk\u0119 losujemy z r\xF3\u017Cnych sytuacji, kursant\xF3w, telefon\xF3w i miesi\u0119cy. Oznaczaj\u0105cy nie widz\u0105 wyniku automatu. Trenerzy ods\u0142uchali ju\u017C dziesi\u0105tki tysi\u0119cy nagra\u0144 \u2014 to du\u017Cy zbi\xF3r ocen od ludzi.` },
  { slug: "ograniczenia", sort: 50, title: "\u26A0\uFE0F Ograniczenia", body: `- Tekst z AI wyg\u0142adza j\u0105kanie: z tekst\xF3w nie policzymy odsetka zaj\u0105kni\u0119\u0107 (%SS).
- Ocena trenera nie jest zwalidowan\u0105 skal\u0105 \u2014 brakuje pomiaru zgodno\u015Bci oceniaj\u0105cych.
- Brak grupy kontrolnej \u2014 mo\u017Cna por\xF3wnywa\u0107 kursant\xF3w zaczynaj\u0105cych w r\xF3\u017Cnych miesi\u0105cach.
- Samoselekcja: najbardziej zmotywowani nagrywaj\u0105 najwi\u0119cej.
- Niepe\u0142ne dane: nie ka\u017Cde nagranie ma samoocen\u0119, odleg\u0142o\u015B\u0107 od domu albo tekst.
- Teksty z telefonu maj\u0105 przybli\u017Cone czasy s\u0142\xF3w (pauzy s\u0105 z pomiaru d\u017Awi\u0119ku).` },
  { slug: "etyka", sort: 60, title: "\u2696\uFE0F Etyka i RODO", body: `- **Zgoda na badania** od ka\u017Cdego kursanta (nieletni \u2014 rodzic), z prawem wycofania; najpro\u015Bciej w PitchRec i w umowie kursu.
- **Umowa o wsp\xF3\u0142pracy i powierzeniu danych** z Politechnik\u0105: cel, zakres, czas przechowywania, dost\u0119p.
- **Opinia komisji bioetycznej** \u2014 zwykle po stronie uczelni.
- **Pseudonimy** s\u0105 ju\u017C w eksporcie; klucz zostaje u nas, wi\u0119c tylko my usuwamy dane osoby, kt\xF3ra wycofa zgod\u0119.
- **Pliki d\u017Awi\u0119kowe** rozpoznaj\u0105 osob\u0119 \u2014 tylko dla os\xF3b z umowy, bez publikowania.
- **Osoby trzecie** (sprzedawcy, przechodnie) te\u017C s\u0105 w nagraniach \u2014 do akustyki lepiej monologi albo wypowiedzi kursanta.` },
  { slug: "kroki", sort: 70, title: "\u{1F4CB} Co zbiera\u0107 teraz i nast\u0119pne kroki", body: `**Do aplikacji (za rok mocne dane):**
- kwestionariusze OASES, S-24, UTBAS-6 \u2014 start, po 3, 6 i 12 miesi\u0105cach (sprawdzi\u0107 polskie wersje i licencje);
- sta\u0142e nagranie pomiarowe: ten sam tekst + 2 minuty monologu, w tych samych terminach;
- podw\xF3jna ocena ok. 5% nagra\u0144 przez dw\xF3ch trener\xF3w;
- zgoda na badania przy rejestracji.

**Nast\u0119pne kroki:**
- [ ] Pobra\u0107 zestawienie danych i dopisa\u0107 liczby do dokumentu
- [ ] Wybra\u0107 z Politechnik\u0105 1\u20132 tematy na start (np. 1 i 6)
- [ ] Umowa o wsp\xF3\u0142pracy i powierzeniu danych
- [ ] Zgoda na badania w PitchRec i w umowie kursu
- [ ] Pr\xF3bny eksport CSV (np. 3 miesi\u0105ce) do oceny przez uczelni\u0119
- [ ] Ustali\u0107 kwestionariusze i nagranie pomiarowe` }
];
async function rsNotesTable(env) {
  await env.DB.prepare("CREATE TABLE IF NOT EXISTS research_notes (slug TEXT PRIMARY KEY, title TEXT, body TEXT, sort INTEGER, updated_by TEXT, updated_at INTEGER, deleted INTEGER DEFAULT 0)").run();
  const now = Math.floor(Date.now() / 1e3);
  await env.DB.batch(RS_NOTES_SEED.map((n) => env.DB.prepare("INSERT OR IGNORE INTO research_notes (slug, title, body, sort, updated_by, updated_at, deleted) VALUES (?, ?, ?, ?, 'claude', ?, 0)").bind(n.slug, n.title, n.body, n.sort, now)));
}
__name(rsNotesTable, "rsNotesTable");
__name2(rsNotesTable, "rsNotesTable");
async function rsNotesList(env) {
  await rsNotesTable(env);
  const rs = await env.DB.prepare("SELECT slug, title, body, sort, updated_by, updated_at FROM research_notes WHERE COALESCE(deleted, 0) = 0 ORDER BY sort, updated_at").all();
  return { ok: true, notes: rs.results || [], doc_url: RS_DOC_URL };
}
__name(rsNotesList, "rsNotesList");
__name2(rsNotesList, "rsNotesList");
async function rsNotesSave(env, who, b) {
  await rsNotesTable(env);
  const title = String(b.title || "").trim().slice(0, 200), body = String(b.body || "").slice(0, 1e5);
  if (!title) return { ok: false, error: "Brak tytu\u0142u" };
  let slug = String(b.slug || "").trim();
  if (!slug) {
    slug = "n-" + Date.now().toString(36);
  }
  const sort = Number.isFinite(Number(b.sort)) ? Number(b.sort) : 1e3;
  await env.DB.prepare("INSERT INTO research_notes (slug, title, body, sort, updated_by, updated_at, deleted) VALUES (?, ?, ?, ?, ?, ?, 0) ON CONFLICT(slug) DO UPDATE SET title = excluded.title, body = excluded.body, sort = excluded.sort, updated_by = excluded.updated_by, updated_at = excluded.updated_at, deleted = 0").bind(slug, title, body, sort, String(who.email || ""), Math.floor(Date.now() / 1e3)).run();
  return { ok: true, slug };
}
__name(rsNotesSave, "rsNotesSave");
__name2(rsNotesSave, "rsNotesSave");
function ofReminderTo(env) {
  return String(env.COURSE_REMINDER_TO || "nowamowa@gmail.com").trim();
}
__name(ofReminderTo, "ofReminderTo");
async function ofTables(env) {
  await env.DB.prepare("CREATE TABLE IF NOT EXISTS course_reminders (id INTEGER PRIMARY KEY AUTOINCREMENT, to_email TEXT, student_name TEXT, start_date TEXT NOT NULL, child INTEGER, created_by TEXT, created_at INTEGER, sent_at INTEGER, cancelled INTEGER DEFAULT 0, error TEXT, deposit_at INTEGER, deposit_by TEXT)").run();
  for (const c of ["deposit_at INTEGER", "deposit_by TEXT", "remind_at INTEGER", "remind_by TEXT"]) {
    try {
      await env.DB.prepare("ALTER TABLE course_reminders ADD COLUMN " + c).run();
    } catch (e) {
    }
  }
}
__name(ofTables, "ofTables");
__name2(ofTables, "ofTables");
function ofMime(o) {
  const imgs = [];
  const html = String(o.html || "").replace(/src=(["'])data:(image\/[a-z+.-]+);base64,([A-Za-z0-9+/=\s]+)\1/gi, (m2, q, type, data) => {
    const cid = "img" + imgs.length + "_" + crypto.randomUUID().slice(0, 8) + "@nowamowa";
    imgs.push({ cid, type, data: data.replace(/\s+/g, "") });
    return "src=" + q + "cid:" + cid + q;
  });
  const bMix = "nsmix_" + crypto.randomUUID().replace(/-/g, ""), bRel = "nsrel_" + crypto.randomUUID().replace(/-/g, "");
  const headers = [
    "From: " + mimeWord(o.fromName || "Nowa Mowa") + (o.from ? " <" + o.from + ">" : ""),
    "To: " + o.to,
    o.cc ? "Cc: " + o.cc : null,
    o.replyTo ? "Reply-To: " + o.replyTo : null,
    "Subject: " + mimeWord(o.subject || ""),
    "MIME-Version: 1.0",
    'Content-Type: multipart/mixed; boundary="' + bMix + '"'
  ].filter(Boolean);
  let m = headers.join("\r\n") + "\r\n\r\n--" + bMix + '\r\nContent-Type: multipart/related; boundary="' + bRel + '"\r\n\r\n--' + bRel + "\r\nContent-Type: text/html; charset=UTF-8\r\nContent-Transfer-Encoding: base64\r\n\r\n" + wrap76(b64Utf8(html)) + "\r\n";
  imgs.forEach((im, i) => {
    const ext = (im.type.split("/")[1] || "png").replace("jpeg", "jpg").replace(/\+.*/, "");
    m += "--" + bRel + "\r\nContent-Type: " + im.type + '; name="obraz' + i + "." + ext + '"\r\nContent-Transfer-Encoding: base64\r\nContent-ID: <' + im.cid + '>\r\nContent-Disposition: inline; filename="obraz' + i + "." + ext + '"\r\n\r\n' + wrap76(im.data) + "\r\n";
  });
  m += "--" + bRel + "--\r\n";
  if (o.pdf) m += "--" + bMix + '\r\nContent-Type: application/pdf; name="' + mimeWord(o.filename || "umowa.pdf") + '"\r\nContent-Disposition: attachment; filename="' + mimeWord(o.filename || "umowa.pdf") + '"\r\nContent-Transfer-Encoding: base64\r\n\r\n' + wrap76(o.pdf) + "\r\n";
  return m + "--" + bMix + "--";
}
__name(ofMime, "ofMime");
__name2(ofMime, "ofMime");
async function ofGmailSend(env, mime) {
  const token = await googleOAuthAccessToken(env);
  if (!token) return { ok: false, error: "Konto Google nie jest po\u0142\u0105czone \u2014 admin: \u2699\uFE0F Admin \u2192 Kalendarz Google \u2192 Po\u0142\u0105cz" };
  const r = await fetch("https://gmail.googleapis.com/upload/gmail/v1/users/me/messages/send?uploadType=media", {
    method: "POST",
    headers: { "Authorization": "Bearer " + token, "Content-Type": "message/rfc822" },
    body: mime
  });
  const d = await r.json().catch(() => ({}));
  if (!r.ok) {
    const msg = d && d.error && d.error.message || "status " + r.status;
    if (r.status === 403 || /insufficient|scope/i.test(msg)) return { ok: false, error: "Po\u0142\u0105czone konto Google nie ma zgody na wysy\u0142anie maili \u2014 \u2699\uFE0F Admin \u2192 Kalendarz Google \u2192 Roz\u0142\u0105cz i po\u0142\u0105cz ponownie. (" + msg + ")" };
    return { ok: false, error: "Gmail: " + msg };
  }
  return { ok: true, id: d.id || null };
}
__name(ofGmailSend, "ofGmailSend");
__name2(ofGmailSend, "ofGmailSend");
var OF_TR_LANG = { sk: "s\u0142owacki", cs: "czeski" };
async function ofTrKey(lang, t) {
  const h = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(lang + "" + t));
  return [...new Uint8Array(h)].map((b) => b.toString(16).padStart(2, "0")).join("");
}
__name(ofTrKey, "ofTrKey");
function ofTagSig(t) {
  return (String(t).match(/<\/?[a-z][a-z0-9]*/gi) || []).map((x) => x.toLowerCase()).join(",");
}
__name(ofTagSig, "ofTagSig");
async function handleOfferTranslate(request, env) {
  let b = {};
  try {
    b = await request.json();
  } catch (e) {
  }
  const who = await contactAuth(env, b.ns_token, b.ns_email, b.ns_server);
  if (!who) return json({ error: "Nieprawid\u0142owy lub wygas\u0142y token NS \u2014 zaloguj si\u0119 ponownie" }, 401);
  if (who.forbidden || !who.admin) return json({ error: "Tylko administrator" }, 403);
  const lang = String(b.lang || "");
  if (!OF_TR_LANG[lang]) return json({ error: "J\u0119zyk: sk albo cs" }, 400);
  const texts = (Array.isArray(b.texts) ? b.texts : []).slice(0, 40).map((x) => String(x == null ? "" : x).slice(0, 8e3));
  await env.DB.prepare("CREATE TABLE IF NOT EXISTS offer_tr (h TEXT PRIMARY KEY, lang TEXT, src TEXT, out TEXT, at INTEGER)").run();
  const keys = await Promise.all(texts.map((t) => ofTrKey(lang, t)));
  const out = new Array(texts.length).fill(null), todo = [];
  for (let i = 0; i < texts.length; i++) {
    if (!/\p{L}{2}/u.test(texts[i].replace(/<[^>]*>/g, ""))) {
      out[i] = texts[i];
      continue;
    }
    const r = await env.DB.prepare("SELECT out FROM offer_tr WHERE h = ?").bind(keys[i]).first();
    if (r && r.out != null) out[i] = r.out;
    else todo.push(i);
  }
  if (todo.length) {
    const L = OF_TR_LANG[lang];
    const sys = `Jeste\u015B zawodowym t\u0142umaczem z j\u0119zyka polskiego na ${L}. T\u0142umaczysz ofert\u0119 kursu, umow\u0119 rezerwacji terminu i maile szko\u0142y \u201ESzko\u0142a Nowej Mowy\u201D (metoda Nowa Mowa\xAE) dla klienta ze ${lang === "sk" ? "S\u0142owacji" : "Czech"}.
Zasady:
- Dostajesz tablic\u0119 JSON z fragmentami HTML. Zwr\xF3\u0107 WY\u0141\u0104CZNIE tablic\u0119 JSON z t\u0142umaczeniami: tyle samo element\xF3w, ta sama kolejno\u015B\u0107.
- Zachowaj dok\u0142adnie wszystkie znaczniki HTML, atrybuty, style i adresy URL; t\u0142umacz tylko tekst widoczny dla czytelnika.
- Nie zmieniaj kwot, walut (z\u0142), dat liczbowych, numer\xF3w kont, telefon\xF3w, e-maili, adres\xF3w, nazw firm (NEW SPEECH\xAE Sp. z o.o., Szko\u0142a Nowej Mowy) ani znaku \xAE. \u201ENowa Mowa\u201D jako nazwa metody zostaje bez t\u0142umaczenia.
- Nazwy miesi\u0119cy i dni tygodnia przet\u0142umacz. Imiona i nazwiska zostaw, zwrot grzeczno\u015Bciowy dopasuj do j\u0119zyka (forma \u201EVy\u201D, np. ${lang === "sk" ? "\u201EV\xE1\u017Een\xE1 pani \u2026\u201D, \u201EV\xE1\u017Een\xFD p\xE1n \u2026\u201D" : "\u201EV\xE1\u017Een\xE1 pan\xED \u2026\u201D, \u201EV\xE1\u017Een\xFD pane \u2026\u201D"}).
- Styl: naturalny, uprzejmy, poprawny ${L}. W umowie i regulaminie precyzyjny j\u0119zyk prawniczy.
- Nigdy nie u\u017Cywaj s\u0142\xF3w ${lang === "sk" ? "\u201Eterapia\u201D, \u201Eterapeutick\xFD\u201D, \u201Eterapeut\u201D" : "\u201Eterapie\u201D, \u201Eterapeutick\xFD\u201D, \u201Eterapeut\u201D"} \u2014 zamiast nich \u201Etr\xE9ning\u201D, \u201Ekurz\u201D, \u201E\u0161pecialista\u201D/\u201Especialista\u201D.`;
    const parts = [];
    let cur = [], len = 0;
    for (const i of todo) {
      if (cur.length && (cur.length >= 14 || len + texts[i].length > 5e3)) {
        parts.push(cur);
        cur = [];
        len = 0;
      }
      cur.push(i);
      len += texts[i].length;
    }
    if (cur.length) parts.push(cur);
    for (const part of parts) {
      const src = part.map((i) => texts[i]);
      let arr = null;
      for (let attempt = 0; attempt < 2 && !arr; attempt++) {
        try {
          const txt = await txLlm(env, sys, JSON.stringify(src), Math.min(8e3, 400 + src.join("").length * 2));
          const a = txt.indexOf("["), z = txt.lastIndexOf("]");
          const p = a >= 0 && z > a ? JSON.parse(txt.slice(a, z + 1)) : null;
          if (Array.isArray(p) && p.length === src.length) arr = p.map((x) => String(x == null ? "" : x));
        } catch (e) {
        }
      }
      if (!arr) continue;
      const now = Math.floor(Date.now() / 1e3);
      for (let k = 0; k < part.length; k++) {
        const i = part[k], t = arr[k];
        if (!t || ofTagSig(t) !== ofTagSig(texts[i])) continue;
        out[i] = t;
        try {
          await env.DB.prepare("INSERT OR REPLACE INTO offer_tr (h, lang, src, out, at) VALUES (?, ?, ?, ?, ?)").bind(keys[i], lang, texts[i].slice(0, 2e3), t, now).run();
        } catch (e) {
        }
      }
    }
  }
  return json({ ok: true, texts: out, missing: out.filter((x) => x == null).length });
}
__name(handleOfferTranslate, "handleOfferTranslate");
__name2(handleOfferTranslate, "handleOfferTranslate");
async function svReplySalt(env) {
  await ensureTranscriptTables(env);
  const r = await env.DB.prepare("SELECT v FROM tx_kv WHERE k = 'reply_salt'").first();
  if (r && r.v) return r.v;
  const b = new Uint8Array(24);
  crypto.getRandomValues(b);
  const v = [...b].map((x) => x.toString(16).padStart(2, "0")).join("");
  await env.DB.prepare("INSERT INTO tx_kv (k, v) VALUES ('reply_salt', ?) ON CONFLICT(k) DO NOTHING").bind(v).run();
  const again = await env.DB.prepare("SELECT v FROM tx_kv WHERE k = 'reply_salt'").first();
  return again && again.v || v;
}
__name(svReplySalt, "svReplySalt");
__name2(svReplySalt, "svReplySalt");
function svB64u(t) {
  return btoa(unescape(encodeURIComponent(t))).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}
__name(svB64u, "svB64u");
function svUnB64u(t) {
  try {
    return decodeURIComponent(escape(atob(String(t || "").replace(/-/g, "+").replace(/_/g, "/"))));
  } catch (e) {
    return "";
  }
}
__name(svUnB64u, "svUnB64u");
async function svReplySig(env, email, key) {
  const k = await crypto.subtle.importKey("raw", new TextEncoder().encode(await svReplySalt(env)), { name: "HMAC", hash: "SHA-256" }, false, ["sign"]);
  const sig = await crypto.subtle.sign("HMAC", k, new TextEncoder().encode(String(email).toLowerCase() + "|" + String(key || "")));
  return [...new Uint8Array(sig)].slice(0, 12).map((b) => b.toString(16).padStart(2, "0")).join("");
}
__name(svReplySig, "svReplySig");
async function svReplyLinks(env, base, email, key) {
  const q = "e=" + svB64u(String(email).toLowerCase()) + "&k=" + svB64u(String(key || "")) + "&s=" + await svReplySig(env, email, key);
  const why = {};
  Object.keys(SV_WHY).forEach((c) => {
    why[c] = base + "/r?" + q + "&a=why_" + c;
  });
  return { keep: base + "/r?" + q + "&a=keep", stop: base + "/r?" + q + "&a=stop", why };
}
__name(svReplyLinks, "svReplyLinks");
var SV_WHY = {
  zapis: ["\u2705", "Chc\u0119 si\u0119 zapisa\u0107 / mam pytania", "Super! \u{1F389}", "<p>Trener odezwie si\u0119, \u017Ceby ustali\u0107 szczeg\xF3\u0142y \u2014 termin, wariant i formalno\u015Bci. Je\u015Bli wolisz rozmow\u0119, zostaw numer telefonu.</p>"],
  cena: ["\u{1F4B0}", "Cena", "Dzi\u0119kujemy za szczero\u015B\u0107 \u{1F64F}", '<p>Koszt kursu bywa przeszkod\u0105 \u2014 dlatego s\u0105 sposoby, \u017Ceby go obni\u017Cy\u0107 albo roz\u0142o\u017Cy\u0107:</p><ul><li><b>Raty</b> \u2014 kurs mo\u017Cna roz\u0142o\u017Cy\u0107 nawet na 60 rat (p\u0142atno\u015B\u0107 ratalna imoje).</li><li><b>Dofinansowanie</b> (PCPR / MOPS / GOPS i inne) \u2014 <a href="https://nowamowa.com/dofinansowania/">jak to zrobi\u0107 krok po kroku</a>.</li><li><b>Fundacja Nowa Mowa</b> wspiera osoby w trudniejszej sytuacji \u2014 <a href="https://nowamowa.org">nowamowa.org</a>.</li><li><b>Trening online</b> \u2014 mo\u017Cna zacz\u0105\u0107 od pojedynczych sesji 50 min.</li></ul>'],
  termin: ["\u{1F4C5}", "Termin", "Dzi\u0119kujemy! \u{1F4C5}", "<p>Kursy startuj\u0105 regularnie. Napisz, kt\xF3ry miesi\u0105c albo dni tygodnia Ci pasuj\u0105 \u2014 trener zaproponuje najbli\u017Cszy dogodny termin.</p>"],
  dojazd: ["\u{1F697}", "Dojazd", "Dzi\u0119kujemy! \u{1F697}", "<p>Kurs stacjonarny odbywa si\u0119 w Miko\u0142owie (Rynek 3/1A). Je\u015Bli dojazd jest trudny, mo\u017Cesz zacz\u0105\u0107 od <b>treningu online</b> \u2014 indywidualne sesje 50 min z trenerem przez internet. Napisz, sk\u0105d doje\u017Cd\u017Casz \u2014 podpowiemy najlepsze rozwi\u0105zanie.</p>"],
  pozniej: ["\u23F3", "Jeszcze nie teraz", "Rozumiemy \u{1F642}", "<p>To wa\u017Cna decyzja i musi przyj\u015B\u0107 w dobrym momencie. Wybierz, kiedy mo\u017Cemy si\u0119 odezwa\u0107 \u2014 do tego czasu nie b\u0119dziemy przypomina\u0107.</p>"],
  online: ["\u{1F4BB}", "Wol\u0119 online", "\u015Awietnie! \u{1F4BB}", "<p>Trening online to <b>indywidualne sesje 50 min</b> z trenerem przez internet. Trener odezwie si\u0119 z propozycj\u0105 termin\xF3w.</p>"],
  inne: ["\u{1F4AC}", "Inny pow\xF3d", "Dzi\u0119kujemy \u{1F64F}", "<p>Napisz, co sprawia trudno\u015B\u0107 \u2014 ka\u017Cda odpowied\u017A pomaga nam lepiej dopasowa\u0107 kurs.</p>"]
};
async function svReplyPage(request, env, url) {
  let p = Object.fromEntries(url.searchParams.entries());
  if (request.method === "POST") {
    try {
      const f = await request.formData();
      for (const [k, v] of f.entries()) p[k] = String(v);
    } catch (e) {
    }
  }
  const email = svUnB64u(p.e).toLowerCase(), key = svUnB64u(p.k);
  const whyC = (String(p.a || "").match(/^why_(\w+)$/) || [])[1], why = whyC && SV_WHY[whyC] ? whyC : null;
  const act = why ? "why_" + why : p.a === "stop" ? "stop" : "keep";
  const page = /* @__PURE__ */ __name((title, body) => new Response(`<!doctype html><html lang="pl"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Nowa Mowa</title><style>body{margin:0;background:#f4f1ed;font-family:Arial,Helvetica,sans-serif;color:#2b2b2b}.w{max-width:560px;margin:0 auto;padding:16px}.h{background:#1E1E1E;color:#fff;padding:16px 20px;border-top:6px solid #F5A623;border-radius:10px 10px 0 0}.h b{color:#F5A623;font-size:12px;letter-spacing:2px}.c{background:#fff;padding:20px;border-radius:0 0 10px 10px;line-height:1.6}button{background:#E8820C;color:#fff;border:0;padding:12px 18px;border-radius:8px;font-size:15px;font-weight:bold;cursor:pointer}button.g{background:#8C7A5C}textarea,input{width:100%;box-sizing:border-box;padding:10px;border:1px solid #E5DCCB;border-radius:8px;font-size:15px;margin:6px 0 12px;font-family:inherit}</style></head><body><div class="w"><div class="h"><b>SZKO\u0141A NOWEJ MOWY</b><div style="font-size:20px;font-weight:bold">\u{1F5E3} Nowa Mowa</div></div><div class="c"><h2 style="margin-top:0">${title}</h2>${body}</div></div></body></html>`, { headers: { "Content-Type": "text/html; charset=utf-8", "Cache-Control": "no-store" } }), "page");
  if (!email || !/^[0-9a-f]{24}$/.test(String(p.s || "")) || p.s !== await svReplySig(env, email, key)) return page("Link jest nieaktualny", "<p>Nie uda\u0142o si\u0119 odczyta\u0107 odpowiedzi. Wystarczy odpisa\u0107 na maila \u2014 przeczytamy wiadomo\u015B\u0107.</p>");
  const hidden = `<input type="hidden" name="e" value="${ofEsc(p.e)}"><input type="hidden" name="k" value="${ofEsc(p.k)}"><input type="hidden" name="s" value="${ofEsc(p.s)}"><input type="hidden" name="a" value="${act}">`;
  if (why) {
    const W = SV_WHY[why];
    const extraF = why === "zapis" ? `<label>Telefon (opcjonalnie)</label><input name="t" inputmode="tel">` : why === "termin" ? `<label>Jaki termin by pasowa\u0142? (np. miesi\u0105c, dni tygodnia)</label><input name="w">` : why === "pozniej" ? `<label>Kiedy mo\u017Cemy si\u0119 odezwa\u0107?</label><select name="z" style="width:100%;padding:10px;border:1px solid #ddd;border-radius:6px;font-size:15px;box-sizing:border-box"><option value="1">za miesi\u0105c</option><option value="3" selected>za 3 miesi\u0105ce</option><option value="6">za p\xF3\u0142 roku</option><option value="12">za rok</option></select>` : why === "dojazd" ? `<label>Sk\u0105d doje\u017Cd\u017Casz? (opcjonalnie)</label><input name="w">` : "";
    if (request.method === "GET") return page(W[0] + " " + W[1], W[3] + `<form method="post">${hidden}${extraF}<label>${why === "inne" ? "Co sprawia trudno\u015B\u0107?" : "Chcesz co\u015B dopisa\u0107? (opcjonalnie)"}</label><textarea name="m" rows="3"></textarea><button type="submit">Wy\u015Blij odpowied\u017A</button></form><p style="font-size:13px;color:#8C7A5C">Odpowied\u017A trafi tylko do trenera.</p>`);
    const z = why === "pozniej" ? Math.max(0, Math.min(24, Number(p.z) || 0)) : 0, w = String(p.w || "").slice(0, 300).trim();
    const msg2 = ((z ? "[za " + z + " mies.] " : "") + (w ? (why === "termin" ? "Termin: " : why === "dojazd" ? "Sk\u0105d: " : "") + w + (p.m ? " \xB7 " : "") : "") + String(p.m || "")).slice(0, 2e3).trim();
    const tel2 = String(p.t || "").slice(0, 40), now2 = Math.floor(Date.now() / 1e3);
    await env.DB.prepare("CREATE TABLE IF NOT EXISTS contact_replies (id INTEGER PRIMARY KEY AUTOINCREMENT, email TEXT, row_key TEXT, action TEXT, message TEXT, phone TEXT, created_at INTEGER)").run();
    const dup2 = await env.DB.prepare("SELECT id FROM contact_replies WHERE email = ? AND action = ? AND created_at > ?").bind(email, act, now2 - 60).first();
    if (!dup2) {
      await env.DB.prepare("INSERT INTO contact_replies (email, row_key, action, message, phone, created_at) VALUES (?, ?, ?, ?, ?, ?)").bind(email, key, act, msg2, tel2, now2).run();
      try {
        await ensureGoogleTables(env);
        const account = await appSettingGet(env, "google_account") || "";
        const html = '<div style="font-family:Arial,Helvetica,sans-serif;font-size:15px;line-height:1.55"><p><b>' + ofEsc(email) + '</b> odpowiedzia\u0142(a) na pytanie o decyzj\u0119:</p><p style="font-size:18px"><b>' + W[0] + " " + ofEsc(W[1]) + "</b></p>" + (tel2 ? "<p>\u{1F4DE} Telefon: <b>" + ofEsc(tel2) + "</b></p>" : "") + (msg2 ? '<blockquote style="border-left:4px solid #F5A623;margin:0;padding:6px 12px;background:#FFF6E8">' + ofEsc(msg2).replace(/\n/g, "<br>") + "</blockquote>" : "") + '<p style="color:#8C7A5C;font-size:13px">Panel trenera \u2192 \u{1F4CB} Ankiety \u2192 filtr \u201E\u2753 pow\xF3d\u201D \u2014 tam wy\u015Blesz dopasowan\u0105 odpowied\u017A jednym klikni\u0119ciem.</p></div>';
        await ofGmailSend(env, ofMime({ from: account, fromName: "Panel Nowej Mowy", to: ofReminderTo(env), replyTo: email, subject: "\u2753 Pow\xF3d: " + W[1] + " \u2014 " + email, html }));
      } catch (e) {
      }
    }
    return page(W[2], (/^(cena|dojazd|online)$/.test(why) ? W[3] : "") + (why === "pozniej" ? "<p>Odezwiemy si\u0119 " + (z === 1 ? "za miesi\u0105c" : z === 12 ? "za rok" : z === 6 ? "za p\xF3\u0142 roku" : "za " + (z || 3) + " miesi\u0105ce") + ". Do tego czasu nie b\u0119dziemy przypomina\u0107.</p>" : "") + "<p><b>Trener dosta\u0142 Twoj\u0105 odpowied\u017A.</b> Dzi\u0119kujemy!</p>");
  }
  if (request.method === "GET") {
    return act === "stop" ? page("Nie chcesz kolejnych wiadomo\u015Bci?", `<p>Po potwierdzeniu nie wy\u015Blemy ju\u017C przypomnie\u0144 ani wiadomo\u015Bci zbiorowych na adres <b>${ofEsc(email)}</b>.</p><form method="post">${hidden}<label>Je\u015Bli chcesz, napisz dlaczego (opcjonalnie):</label><textarea name="m" rows="3"></textarea><button class="g" type="submit">Potwierdzam \u2014 nie chc\u0119 wiadomo\u015Bci</button></form>`) : page("Super \u2014 zosta\u0144my w kontakcie! \u{1F642}", `<p>Trener odezwie si\u0119 do Ciebie. Mo\u017Cesz dopisa\u0107, kiedy najlepiej zadzwoni\u0107 albo o co chcesz zapyta\u0107:</p><form method="post">${hidden}<label>Telefon (opcjonalnie)</label><input name="t" inputmode="tel"><label>Wiadomo\u015B\u0107 dla trenera (opcjonalnie)</label><textarea name="m" rows="4"></textarea><button type="submit">\u2705 Chc\u0119 by\u0107 w kontakcie z trenerem</button></form>`);
  }
  const msg = String(p.m || "").slice(0, 2e3), tel = String(p.t || "").slice(0, 40), now = Math.floor(Date.now() / 1e3);
  await env.DB.prepare("CREATE TABLE IF NOT EXISTS contact_replies (id INTEGER PRIMARY KEY AUTOINCREMENT, email TEXT, row_key TEXT, action TEXT, message TEXT, phone TEXT, created_at INTEGER)").run();
  const dup = await env.DB.prepare("SELECT id FROM contact_replies WHERE email = ? AND action = ? AND created_at > ?").bind(email, act, now - 60).first();
  if (!dup) {
    await env.DB.prepare("INSERT INTO contact_replies (email, row_key, action, message, phone, created_at) VALUES (?, ?, ?, ?, ?, ?)").bind(email, key, act, msg, tel, now).run();
    if (act === "stop" && key) {
      try {
        await sstTable(env);
        await env.DB.prepare("INSERT INTO survey_status (row_key, no_contact, updated_by, updated_at) VALUES (?, 1, ?, ?) ON CONFLICT(row_key) DO UPDATE SET no_contact = 1, updated_by = excluded.updated_by, updated_at = excluded.updated_at").bind(key, "odpowied\u017A z maila", now).run();
      } catch (e) {
      }
    }
    try {
      await ensureGoogleTables(env);
      const account = await appSettingGet(env, "google_account") || "";
      const subj = (act === "stop" ? "\u{1F6AB} Rezygnacja z wiadomo\u015Bci: " : "\u2705 Chce kontaktu z trenerem: ") + email;
      const html = '<div style="font-family:Arial,Helvetica,sans-serif;font-size:15px;line-height:1.55">' + (act === "stop" ? "<p><b>" + ofEsc(email) + "</b> nie chce kolejnych wiadomo\u015Bci. W panelu ustawiono \u201E\u{1F6AB} nie kontaktowa\u0107\u201D.</p>" : "<p><b>" + ofEsc(email) + "</b> chce by\u0107 w kontakcie z trenerem.</p>") + (tel ? "<p>\u{1F4DE} Telefon: <b>" + ofEsc(tel) + "</b></p>" : "") + (msg ? '<p>\u{1F4AC} Wiadomo\u015B\u0107:</p><blockquote style="border-left:4px solid #F5A623;margin:0;padding:6px 12px;background:#FFF6E8">' + ofEsc(msg).replace(/\n/g, "<br>") + "</blockquote>" : "") + '<p style="color:#8C7A5C;font-size:13px">Ankieta: ' + ofEsc(key || "\u2014") + " \xB7 panel trenera \u2192 \u{1F4CB} Ankiety</p></div>";
      await ofGmailSend(env, ofMime({ from: account, fromName: "Panel Nowej Mowy", to: ofReminderTo(env), replyTo: email, subject: subj, html }));
    } catch (e) {
    }
  }
  return act === "stop" ? page("Dzi\u0119kujemy \u{1F64F}", "<p>Nie wy\u015Blemy ju\u017C kolejnych wiadomo\u015Bci. Gdyby\u015B kiedy\u015B chcia\u0142(a) wr\xF3ci\u0107 do tematu \u2014 wystarczy napisa\u0107.</p>") : page("Dzi\u0119kujemy! \u{1F642}", "<p>Trener dosta\u0142 Twoj\u0105 odpowied\u017A i wkr\xF3tce si\u0119 odezwie. Trzymamy kciuki!</p>");
}
__name(svReplyPage, "svReplyPage");
__name2(svReplyPage, "svReplyPage");
function ofRatyPage(url) {
  let d = {};
  try {
    const b = String(url.searchParams.get("d") || "").replace(/-/g, "+").replace(/_/g, "/");
    d = JSON.parse(decodeURIComponent(escape(atob(b))));
  } catch (e) {
  }
  const V = (Array.isArray(d.v) ? d.v : []).slice(0, 8).map((x) => [Math.max(0, Math.min(99, Number(x && x[0]) || 0)), String(x && x[1] || "").slice(0, 60), Math.max(0, Math.min(1e6, Number(x && x[2]) || 0))]).filter((x) => x[2] > 0);
  const r = Math.max(0, Math.min(60, Number(d.r) || 21.97)), sel = Math.max(0, Number(d.s) || 0);
  const link = /^https:\/\/[^\s"'<>]+$/.test(String(d.l || "")) ? String(d.l) : "";
  const data = JSON.stringify({ v: V, r, s: sel, l: link }).replace(/</g, "\\u003c");
  const html = `<!doctype html><html lang="pl"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Kalkulator rat \u2014 Nowa Mowa</title>
<style>body{margin:0;background:#f4f1ed;font-family:Arial,Helvetica,sans-serif;color:#222}.w{max-width:620px;margin:0 auto;padding:16px}.h{background:#1E1E1E;color:#fff;padding:18px 20px;border-top:6px solid #F5A623}.h b{color:#F5A623;font-size:12px;letter-spacing:1px;text-transform:uppercase}.h div{font-size:22px;font-weight:bold;margin-top:4px}
.c{background:#fff;padding:16px 20px;margin-top:12px;border:1px solid #ead8c4}.vs{display:flex;flex-wrap:wrap;gap:6px;margin:6px 0 14px}.vs button{border:1px solid #ead8c4;background:#fbf8f5;padding:8px 10px;border-radius:6px;cursor:pointer;font-size:13px;text-align:left}.vs button.on{border:2px solid #E8A317;background:#FFF6E0;font-weight:bold}
input[type=range]{width:100%;accent-color:#D9822B;height:28px}.big{font-size:34px;font-weight:bold;color:#D9822B}.row{display:flex;justify-content:space-between;align-items:baseline;gap:10px;flex-wrap:wrap}.sub{color:#6b6259;font-size:13px}.btn{display:inline-block;background:#D9822B;color:#fff;padding:12px 18px;border-radius:6px;text-decoration:none;font-weight:bold;margin-top:12px}
table{width:100%;border-collapse:collapse;font-size:14px;margin-top:10px}td{padding:6px 8px;border-bottom:1px solid #efe4d8}</style></head><body><div class="w">
<div class="h"><b>Szko\u0142a Nowej Mowy</b><div>\u{1F9EE} Kalkulator rat</div></div>
<div class="c"><div class="sub">Wybierz wariant kursu:</div><div class="vs" id="vs"></div>
<div class="row"><span>Liczba rat: <b id="nv"></b></span><span class="sub">od 3 do 60</span></div>
<input type="range" id="n" min="3" max="60" value="12">
<div class="row" style="margin-top:8px"><span class="big" id="rata"></span><span class="sub" id="tot"></span></div>
<table id="tb"></table>
<div id="lk"></div>
<p class="sub" style="font-size:12px;margin-top:14px">Wyliczenia s\u0105 orientacyjne (RRSO ok. <span id="rr"></span>%), jak w kalkulatorze imoje. Ostateczne warunki i decyzj\u0119 kredytow\u0105 przedstawia operator p\u0142atno\u015Bci przed akceptacj\u0105 umowy.</p></div>
<p class="sub" style="text-align:center;font-size:12px">Szko\u0142a Nowej Mowy \xB7 Rynek 3/1A, Miko\u0142\xF3w \xB7 <a href="https://www.nowamowa.com" style="color:#D9822B">nowamowa.com</a></p></div>
<script>var D=${data};var cur=0;
function rata(P,n){var rr=D.r/100;if(!(rr>0))return P/n;var q=Math.pow(1+rr,1/12)-1;return P*q/(1-Math.pow(1+q,-n));}
function zl(x){return (Math.round(x*100)/100).toLocaleString('pl-PL',{minimumFractionDigits:2,maximumFractionDigits:2})+'\xA0z\u0142';}
function zl0(x){return String(Math.round(x)).replace(/\\B(?=(\\d{3})+(?!\\d))/g,'\xA0')+'\xA0z\u0142';}
function esc(t){return String(t).replace(/[&<>"]/g,function(c){return {'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;'}[c];});}
function draw(){var v=D.v[cur];if(!v){document.getElementById('rata').textContent='\u2014';return;}var n=+document.getElementById('n').value,a=rata(v[2],n);
document.getElementById('nv').textContent=n;document.getElementById('rata').textContent=zl(a)+' / mies.';document.getElementById('tot').textContent='razem '+zl(a*n)+' \xB7 cena kursu '+zl0(v[2]);
document.getElementById('tb').innerHTML='<tr class="sub"><td>Liczba rat</td><td>Rata</td><td>Razem</td></tr>'+[6,12,24,36,48,60].map(function(k){var b=rata(v[2],k);return '<tr'+(k===n?' style="background:#FFF6E0;font-weight:bold"':'')+'><td>'+k+'</td><td>'+zl(b)+'</td><td>'+zl(b*k)+'</td></tr>';}).join('');
document.querySelectorAll('#vs button').forEach(function(b,i){b.className=i===cur?'on':'';});}
document.getElementById('vs').innerHTML=D.v.map(function(v,i){return '<button type="button" data-i="'+i+'">'+(v[0]?'Wariant '+v[0]+'<br>':'')+esc(v[1])+'<br><span class="sub">'+zl0(v[2])+'</span></button>';}).join('');
document.querySelectorAll('#vs button').forEach(function(b){b.onclick=function(){cur=+b.getAttribute('data-i');draw();};});
D.v.forEach(function(v,i){if(v[0]===D.s)cur=i;});
document.getElementById('rr').textContent=String(D.r).replace('.',',');
document.getElementById('lk').innerHTML=D.l?'<a class="btn" href="'+esc(D.l)+'">\u{1F4B3} Przejd\u017A do p\u0142atno\u015Bci ratalnej imoje</a>':'<p class="sub" style="margin-top:12px">Zainteresowana/y ratami? Odpisz na maila z ofert\u0105 \u2014 prze\u015Blemy link do p\u0142atno\u015Bci.</p>';
document.getElementById('n').oninput=draw;draw();<\/script></body></html>`;
  return new Response(html, { headers: { "Content-Type": "text/html; charset=utf-8", "Cache-Control": "public, max-age=3600" } });
}
__name(ofRatyPage, "ofRatyPage");
__name2(ofRatyPage, "ofRatyPage");
async function handleOfferSend(request, env) {
  let b = {};
  try {
    b = await request.json();
  } catch (e) {
  }
  const who = await contactAuth(env, b.ns_token, b.ns_email, b.ns_server);
  if (!who) return json({ error: "Nieprawid\u0142owy lub wygas\u0142y token NS \u2014 zaloguj si\u0119 ponownie" }, 401);
  if (who.forbidden || !who.admin) return json({ error: "Tylko administrator" }, 403);
  const to = String(b.to || "").trim();
  if (!/^[^\s@,;<>]+@[^\s@,;<>]+\.[a-z]{2,}$/i.test(to)) return json({ error: "Nieprawid\u0142owy adres e-mail kursanta" }, 400);
  const pdf = String(b.pdf_base64 || "").replace(/\s+/g, "");
  if (pdf.length > 14e6 || String(b.html || "").length > 8e6) return json({ error: "Wiadomo\u015B\u0107 jest za du\u017Ca" }, 400);
  await ensureGoogleTables(env);
  const account = await appSettingGet(env, "google_account") || "";
  const subject = String(b.subject || "Nowa Mowa").slice(0, 200);
  const mime = ofMime({
    from: account,
    fromName: "Nowa Mowa",
    to,
    cc: b.cc_me ? who.email : null,
    replyTo: who.email,
    subject,
    html: b.html,
    pdf: pdf || null,
    filename: String(b.filename || "umowa.pdf").replace(/[^\w.\-ąćęłńóśźżĄĆĘŁŃÓŚŹŻ ]/g, "_").slice(0, 120)
  });
  const s = await ofGmailSend(env, mime);
  if (!s.ok) return json({ error: s.error }, 502);
  try {
    await env.DB.prepare("CREATE TABLE IF NOT EXISTS email_log (id INTEGER PRIMARY KEY AUTOINCREMENT, ns_student_id TEXT, to_email TEXT, subject TEXT, sent_by TEXT, gmail_id TEXT, created_at INTEGER)").run();
    await env.DB.prepare("INSERT INTO email_log (ns_student_id, to_email, subject, sent_by, gmail_id, created_at) VALUES (?, ?, ?, ?, ?, ?)").bind("", to, subject, who.email, s.id, Math.floor(Date.now() / 1e3)).run();
  } catch (e) {
  }
  try {
    const m = b.meta || {};
    if (m.kind === "offer" || m.kind === "contract") {
      await sstTable(env);
      const r0 = await env.DB.prepare("INSERT INTO offer_meta (to_email, kind, variant, variant_name, price, sent_by, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)").bind(to.toLowerCase(), m.kind, Number(m.variant) || null, String(m.variant_name || "").slice(0, 120), Number(m.price) || null, who.email, Math.floor(Date.now() / 1e3)).run();
      const id = r0.meta && r0.meta.last_row_id;
      if (id && env.RECORDINGS) {
        const base = "offers/" + id + "-" + crypto.randomUUID().slice(0, 8);
        await env.RECORDINGS.put(base + ".html", '<!doctype html><html lang="pl"><head><meta charset="utf-8"><title>' + ofEsc(subject) + '</title></head><body style="margin:0;padding:16px;background:#f4f1ed">' + String(b.html || "") + "</body></html>", { httpMetadata: { contentType: "text/html; charset=utf-8" } });
        let pk = null;
        if (pdf) {
          const bin = Uint8Array.from(atob(pdf), (c) => c.charCodeAt(0));
          pk = base + ".pdf";
          await env.RECORDINGS.put(pk, bin, { httpMetadata: { contentType: "application/pdf" } });
        }
        await env.DB.prepare("UPDATE offer_meta SET html_key = ?, pdf_key = ?, subject = ?, pdf_name = ? WHERE id = ?").bind(base + ".html", pk, subject, pk ? String(b.filename || "umowa.pdf").slice(0, 120) : null, id).run();
      }
    }
  } catch (e) {
  }
  let reminder = null;
  const rm = b.reminder;
  if (rm && /^\d{4}-\d{2}-\d{2}$/.test(String(rm.start || ""))) {
    await ofTables(env);
    await env.DB.prepare("UPDATE course_reminders SET cancelled = 1 WHERE LOWER(to_email) = LOWER(?) AND start_date = ? AND sent_at IS NULL AND deposit_at IS NULL AND COALESCE(cancelled, 0) = 0").bind(to, rm.start).run();
    const r = await env.DB.prepare("INSERT INTO course_reminders (to_email, student_name, start_date, child, created_by, created_at) VALUES (?, ?, ?, ?, ?, ?)").bind(to, String(rm.name || "").slice(0, 120), rm.start, rm.child ? 1 : 0, who.email, Math.floor(Date.now() / 1e3)).run();
    reminder = { id: r.meta && r.meta.last_row_id, start: rm.start, on: ofAddDays(rm.start, -7), to: ofReminderTo(env) };
  }
  return json({ ok: true, to, id: s.id, from: account, reminder });
}
__name(handleOfferSend, "handleOfferSend");
__name2(handleOfferSend, "handleOfferSend");
function ofAddDays(ymd, n) {
  const d = /* @__PURE__ */ new Date(ymd + "T12:00:00Z");
  d.setUTCDate(d.getUTCDate() + n);
  return d.toISOString().slice(0, 10);
}
__name(ofAddDays, "ofAddDays");
var OF_MONTHS = ["stycznia", "lutego", "marca", "kwietnia", "maja", "czerwca", "lipca", "sierpnia", "wrze\u015Bnia", "pa\u017Adziernika", "listopada", "grudnia"];
function ofLong(ymd) {
  const d = /* @__PURE__ */ new Date(ymd + "T12:00:00Z");
  return d.getUTCDate() + " " + OF_MONTHS[d.getUTCMonth()];
}
__name(ofLong, "ofLong");
function ofCourseInfoText(start) {
  return "Witaj!\n\nJu\u017C " + ofLong(start) + " rozpoczynamy Kurs Nowej Mowy. Poni\u017Cej przesy\u0142amy najwa\u017Cniejsze informacje organizacyjne dotycz\u0105ce dop\u0142aty do kursu oraz przygotowania do zaj\u0119\u0107.\n\nP\u0142atno\u015Bci\n\n* Przelew: je\u015Bli planujesz op\u0142aci\u0107 kurs przelewem, prosimy o uregulowanie wp\u0142aty najp\xF3\u017Aniej do " + ofLong(ofAddDays(start, -3)) + " na ten sam numer konta, na kt\xF3ry wp\u0142acany by\u0142 zadatek.\n* Got\xF3wka: w przypadku p\u0142atno\u015Bci got\xF3wk\u0105 na miejscu, prosimy o przybycie do Szko\u0142y Nowa Mowa o godzinie 8:50.\n\nCo nale\u017Cy ze sob\u0105 zabra\u0107\n\n* Notes: b\u0119dzie potrzebny do komunikacji z trenerem w trakcie tzw. dni \u201Ekartkowych\u201D.\n* Wygodne obuwie i ciep\u0142e ubranie: planujemy treningi w przestrzeni miejskiej, dlatego przydadz\u0105 si\u0119 wygodne buty oraz co\u015B cieplejszego do ubrania na wypadek ch\u0142odniejszej pogody.\n\nDo zobaczenia na zaj\u0119ciach! W razie jakichkolwiek pyta\u0144 zapraszamy do kontaktu.\n\nPARKINGI\nhttps://www.google.com/maps/d/u/0/edit?mid=1H5gc1803KRf8s34QoN37T_PXqxMkoWA&usp=sharing";
}
__name(ofCourseInfoText, "ofCourseInfoText");
__name2(ofCourseInfoText, "ofCourseInfoText");
function ofCourseInfoHtml(start) {
  const L = "color:#1a4fd6;";
  return '<div style="font-family:Arial,Helvetica,sans-serif;font-size:15px;line-height:1.55;color:#222"><p>Witaj!</p><p>Ju\u017C <b>' + ofLong(start) + '</b> rozpoczynamy Kurs Nowej Mowy. Poni\u017Cej przesy\u0142amy najwa\u017Cniejsze informacje organizacyjne dotycz\u0105ce dop\u0142aty do kursu oraz przygotowania do zaj\u0119\u0107.</p><p style="margin-top:16px"><b>P\u0142atno\u015Bci</b></p><ul style="margin:4px 0 12px 18px;padding:0"><li><b>Przelew:</b> je\u015Bli planujesz op\u0142aci\u0107 kurs przelewem, prosimy o uregulowanie wp\u0142aty najp\xF3\u017Aniej do <b>' + ofLong(ofAddDays(start, -3)) + '</b> na ten sam numer konta, na kt\xF3ry wp\u0142acany by\u0142 zadatek.</li><li><b>Got\xF3wka:</b> w przypadku p\u0142atno\u015Bci got\xF3wk\u0105 na miejscu, prosimy o przybycie do Szko\u0142y Nowa Mowa o godzinie <b>8:50</b>.</li></ul><p><b>Co nale\u017Cy ze sob\u0105 zabra\u0107</b></p><ul style="margin:4px 0 12px 18px;padding:0"><li><b>Notes:</b> b\u0119dzie potrzebny do komunikacji z trenerem w trakcie tzw. dni \u201Ekartkowych\u201D.</li><li><b>Wygodne obuwie i ciep\u0142e ubranie:</b> planujemy treningi w przestrzeni miejskiej, dlatego przydadz\u0105 si\u0119 wygodne buty oraz co\u015B cieplejszego do ubrania na wypadek ch\u0142odniejszej pogody.</li></ul><p>Do zobaczenia na zaj\u0119ciach! W razie jakichkolwiek pyta\u0144 zapraszamy do kontaktu.</p><p><b>PARKINGI</b><br><a href="https://www.google.com/maps/d/u/0/edit?mid=1H5gc1803KRf8s34QoN37T_PXqxMkoWA&usp=sharing" style="' + L + '">Mapa parking\xF3w w pobli\u017Cu Szko\u0142y Nowej Mowy</a></p><p>Szko\u0142a Nowej Mowy<br>Rynek 3/1A, Miko\u0142\xF3w</p></div>';
}
__name(ofCourseInfoHtml, "ofCourseInfoHtml");
__name2(ofCourseInfoHtml, "ofCourseInfoHtml");
async function ofSendCourseInfo(env, r) {
  const account = await appSettingGet(env, "google_account") || "";
  const subj = "Kurs Nowej Mowy od " + ofLong(r.start_date) + " \u2014 dop\u0142ata i przygotowanie";
  const mime = ofMime({ from: account, fromName: "Nowa Mowa", to: r.to_email, cc: ofReminderTo(env), replyTo: account || null, subject: subj, html: ofCourseInfoHtml(r.start_date) });
  const s = await ofGmailSend(env, mime);
  if (s.ok) await env.DB.prepare("UPDATE course_reminders SET sent_at = ?, error = NULL WHERE id = ?").bind(Math.floor(Date.now() / 1e3), r.id).run();
  else await env.DB.prepare("UPDATE course_reminders SET error = ? WHERE id = ?").bind(String(s.error).slice(0, 300), r.id).run();
  return s;
}
__name(ofSendCourseInfo, "ofSendCourseInfo");
__name2(ofSendCourseInfo, "ofSendCourseInfo");
function ofEsc(s) {
  return String(s == null ? "" : s).replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
}
__name(ofEsc, "ofEsc");
async function ofReminderTick(env) {
  const now = /* @__PURE__ */ new Date();
  const p = new Intl.DateTimeFormat("sv-SE", { timeZone: "Europe/Warsaw", year: "numeric", month: "2-digit", day: "2-digit", hour: "2-digit", hourCycle: "h23" }).formatToParts(now);
  const gp = /* @__PURE__ */ __name((t) => (p.find((x) => x.type === t) || {}).value, "gp");
  const today = gp("year") + "-" + gp("month") + "-" + gp("day");
  if (Number(gp("hour")) < 7) return;
  try {
    await ofTables(env);
    const rows = (await env.DB.prepare("SELECT * FROM course_reminders WHERE sent_at IS NULL AND COALESCE(cancelled, 0) = 0 AND start_date >= ? ORDER BY start_date LIMIT 20").bind(today).all()).results || [];
    for (const r of rows) {
      if (!r.deposit_at && !r.remind_at) continue;
      if (ofAddDays(r.start_date, -7) > today) continue;
      const res = await ofSendCourseInfo(env, r);
      if (!res.ok) break;
    }
  } catch (e) {
  }
  try {
    if (/spreadsheets(\s|$)/.test(await appSettingGet(env, "google_scopes") || "") && await appSettingGet(env, "survey_export_day") !== today) {
      await appSettingSet(env, "survey_export_day", today);
      await svExportSheet(env);
    }
  } catch (e) {
  }
}
__name(ofReminderTick, "ofReminderTick");
__name2(ofReminderTick, "ofReminderTick");
var SURVEY_SHEET_DEFAULT = "1AWb_-zkarsGS90oQX8KTQSFJT0INEhVkO-TaTc9xhO8";
async function svSheetId(env) {
  return await appSettingGet(env, "survey_sheet_id") || SURVEY_SHEET_DEFAULT;
}
__name(svSheetId, "svSheetId");
__name2(svSheetId, "svSheetId");
async function svGoogle(env, path, opt) {
  const token = await googleOAuthAccessToken(env);
  if (!token) return { ok: false, code: "no_google", error: "Konto Google nie jest po\u0142\u0105czone \u2014 \u2699\uFE0F Admin \u2192 Kalendarz Google \u2192 Po\u0142\u0105cz." };
  const r = await fetch("https://sheets.googleapis.com/v4/spreadsheets/" + path, opt && opt.method ? { method: opt.method, headers: { Authorization: "Bearer " + token, "Content-Type": "application/json" }, body: JSON.stringify(opt.body || {}) } : { headers: { Authorization: "Bearer " + token } });
  const d = await r.json().catch(() => ({}));
  if (r.ok) return { ok: true, data: d };
  const msg = d && d.error && d.error.message || "status " + r.status;
  if (/has not been used|is disabled|SERVICE_DISABLED|accessNotConfigured/i.test(msg)) {
    const m = msg.match(/https:\/\/console\.developers\.google\.com\/\S+?(?=\s|$)/);
    return { ok: false, code: "api_disabled", error: "W Google Cloud nie jest w\u0142\u0105czone Google Sheets API.", link: m ? m[0] : "https://console.cloud.google.com/apis/library/sheets.googleapis.com" };
  }
  if (r.status === 403 && /insufficient|scope/i.test(msg)) return { ok: false, code: "scope", error: "Po\u0142\u0105czone konto Google nie ma zgody na arkusze (odczyt i zapis) \u2014 po\u0142\u0105cz je ponownie (\u2699\uFE0F Admin \u2192 Kalendarz Google \u2192 Roz\u0142\u0105cz \u2192 Po\u0142\u0105cz) i zaznacz dost\u0119p do arkuszy." };
  if (r.status === 403) return { ok: false, code: "no_access", error: "Po\u0142\u0105czone konto Google nie ma dost\u0119pu do tego arkusza \u2014 udost\u0119pnij arkusz temu kontu (Udost\u0119pnij \u2192 dodaj adres \u2192 Przegl\u0105daj\u0105cy)." };
  if (r.status === 404) return { ok: false, code: "not_found", error: "Nie znaleziono arkusza \u2014 sprawd\u017A link." };
  return { ok: false, code: "other", error: "Google Sheets: " + msg };
}
__name(svGoogle, "svGoogle");
__name2(svGoogle, "svGoogle");
async function svCheck(env) {
  await ensureGoogleTables(env);
  const id = await svSheetId(env), account = await appSettingGet(env, "google_account");
  const scopes = await appSettingGet(env, "google_scopes") || "";
  const meta = await svGoogle(env, encodeURIComponent(id) + "?fields=properties.title,sheets.properties(title,gridProperties)");
  if (!meta.ok) return { ok: false, account, sheet_id: id, scope_ok: /spreadsheets/.test(scopes), ...meta };
  const sheets = (meta.data.sheets || []).map((s) => ({ title: s.properties.title, rows: s.properties.gridProperties && s.properties.gridProperties.rowCount, cols: s.properties.gridProperties && s.properties.gridProperties.columnCount }));
  let filled = null, headers = 0;
  if (sheets.length) {
    const v = await svGoogle(env, encodeURIComponent(id) + "/values/" + encodeURIComponent("'" + sheets[0].title.replace(/'/g, "''") + "'!A1:A5000") + "?majorDimension=COLUMNS");
    if (v.ok) filled = Math.max(0, ((v.data.values || [])[0] || []).filter((x) => String(x || "").trim()).length - 1);
    const h = await svGoogle(env, encodeURIComponent(id) + "/values/" + encodeURIComponent("'" + sheets[0].title.replace(/'/g, "''") + "'!1:1"));
    if (h.ok) headers = ((h.data.values || [])[0] || []).length;
  }
  return {
    ok: true,
    account,
    sheet_id: id,
    title: meta.data.properties && meta.data.properties.title,
    sheets,
    responses: filled,
    columns: headers,
    write_ok: /spreadsheets(\s|$)/.test(scopes),
    export_at: Number(await appSettingGet(env, "survey_export_at")) || null,
    extras: await svCheckExtras(env, headers)
  };
}
__name(svCheck, "svCheck");
__name2(svCheck, "svCheck");
async function svCheckExtras(env, mainCols) {
  const out = [];
  for (const x of await svExtraSheets(env)) {
    const R = await svRowsOne(env, x.id);
    if (!R.ok) {
      out.push(Object.assign({}, x, { ok: false, error: R.error }));
      continue;
    }
    const H = R.header || [];
    let mp = null;
    try {
      mp = JSON.parse(await appSettingGet(env, "svmap_" + x.id) || "null");
    } catch (e) {
    }
    out.push(Object.assign({}, x, { ok: true, responses: R.rows.length, columns: H.length, same_layout: true, mapped: mp && mp.map ? Object.keys(mp.map).length : null, manual: !!(mp && mp.manual) }));
  }
  return out;
}
__name(svCheckExtras, "svCheckExtras");
__name2(svCheckExtras, "svCheckExtras");
function svKey(row) {
  return String(row[0] || "").trim() + "|" + String(row[1] || "").trim().toLowerCase();
}
__name(svKey, "svKey");
__name2(svKey, "svKey");
var SV_FIRST = new Set("adam adrian agata agnieszka aleksander aleksandra alicja amelia aneta angelika anita anna antoni antonina arkadiusz artur barbara bartek bart\u0142omiej bartosz beata bogdan bogus\u0142aw bo\u017Cena cezary czes\u0142aw damian daniel daria dariusz dawid dominik dominika dorota edyta el\u017Cbieta emil emilia ewa ewelina filip franciszek gabriel gabriela gra\u017Cyna grzegorz halina hanna helena henryk hubert igor ilona irena iwona izabela jacek jadwiga jakub jan janina janusz jaros\u0142aw jerzy joanna jolanta j\xF3zef julia julian justyna kacper kamil kamila karina karol karolina katarzyna kazimierz kinga klaudia konrad krystian krystyna krzysztof lena leon leszek liliana lucyna \u0142ucja \u0142ukasz maciej magdalena maja malwina ma\u0142gorzata marcel marcin marek maria marian mariusz marlena marta martyna mateusz micha\u0142 michalina miko\u0142aj milena mi\u0142osz miros\u0142aw monika natalia natan nikola nikodem norbert ola oliwia oliwier oskar patrycja patryk paula paulina pawe\u0142 piotr przemys\u0142aw rados\u0142aw rafa\u0142 renata robert roksana roman ryszard sabina sandra sebastian stanis\u0142aw stefan sylwia szymon tadeusz teresa tomasz urszula wanda weronika wies\u0142aw wiktor wiktoria witold w\u0142adys\u0142aw wojciech zbigniew zdzis\u0142aw zenon zofia zuzanna".split(" "));
var SV_FEM_A_EXC = /* @__PURE__ */ new Set(["kuba", "barnaba", "bonawentura", "kosma", "jarema", "dyzma"]);
function svIsFirst(w) {
  return SV_FIRST.has(String(w || "").toLowerCase());
}
__name(svIsFirst, "svIsFirst");
function svCap(w) {
  w = String(w || "");
  return w ? w.charAt(0).toUpperCase() + w.slice(1).toLowerCase() : w;
}
__name(svCap, "svCap");
function svFemName(first) {
  const l = String(first || "").toLowerCase();
  return /a$/.test(l) && !SV_FEM_A_EXC.has(l);
}
__name(svFemName, "svFemName");
function svSurnameFor(sur, fem) {
  return String(sur || "").split("-").map((p) => {
    const l = p.toLowerCase();
    if (fem) {
      if (/(sk|ck|dzk)i$/.test(l)) return p.slice(0, -1) + "a";
      return p;
    }
    if (/(sk|ck|dzk)a$/.test(l)) return p.slice(0, -1) + "i";
    return p;
  }).join("-");
}
__name(svSurnameFor, "svSurnameFor");
function svParentSurname(parent, email) {
  const w = String(parent || "").replace(/[^\p{L}\s-]/gu, " ").split(/\s+/).filter(Boolean);
  if (w.length >= 2) {
    if (svIsFirst(w[0])) return w.slice(1).join(" ");
    if (svIsFirst(w[w.length - 1])) return w.slice(0, -1).join(" ");
    return w.slice(1).join(" ");
  }
  if (w.length === 1 && !svIsFirst(w[0])) return w[0];
  const tok = String(email || "").split("@")[0].split(/[._\-\d]+/).filter((x) => x.length >= 3 && /^\p{L}+$/u.test(x));
  if (tok.length >= 2) {
    const s = tok.find((x) => !svIsFirst(x));
    if (s && tok.some((x) => svIsFirst(x))) return svCap(s);
  }
  return "";
}
__name(svParentSurname, "svParentSurname");
function svNameOf(row) {
  const t = /* @__PURE__ */ __name((i) => String(row[i] || "").replace(/\s+/g, " ").trim(), "t");
  const child = /rodzic|rodič/i.test(t(2)) || !!t(21);
  if (child) {
    const kid = t(21), parent = t(20);
    if (kid.split(" ").length >= 2 || !kid) return { child: true, name: kid, parent };
    const sur = svParentSurname(parent, t(1));
    return { child: true, name: (kid + (sur ? " " + svSurnameFor(sur, svFemName(kid)) : "")).trim(), parent, surname_added: !!sur };
  }
  return { child: false, name: t(3), parent: "" };
}
__name(svNameOf, "svNameOf");
__name2(svNameOf, "svNameOf");
async function svLinksTable(env) {
  await env.DB.prepare("CREATE TABLE IF NOT EXISTS survey_links (row_key TEXT PRIMARY KEY, ns_student_id TEXT, linked_by TEXT, linked_at INTEGER)").run();
}
__name(svLinksTable, "svLinksTable");
__name2(svLinksTable, "svLinksTable");
var SV_PANEL_TAB = "Panel trenera";
async function svExtraSheets(env) {
  try {
    const a = JSON.parse(await appSettingGet(env, "survey_sheets_extra") || "[]");
    return Array.isArray(a) ? a.filter((x) => x && x.id) : [];
  } catch (e) {
    return [];
  }
}
__name(svExtraSheets, "svExtraSheets");
__name2(svExtraSheets, "svExtraSheets");
async function svRows(env) {
  const main = await svRowsOne(env, await svSheetId(env));
  if (!main.ok) return main;
  const extras = await svExtraSheets(env), seen = new Set(main.rows.map(svKey)), errors = [];
  for (const x of extras) {
    const R = await svRowsOne(env, x.id);
    if (!R.ok) {
      errors.push({ id: x.id, label: x.label, error: R.error });
      continue;
    }
    const M = await svColMap(env, x, R.header, main.header);
    const extraIdx = {};
    (R.header || []).forEach((h, j) => {
      if (M.map[j] == null && String(h || "").trim()) {
        extraIdx[j] = main.header.length;
        main.header.push((x.lang === "cs" ? "\u{1F1E8}\u{1F1FF} " : "\u{1F1F8}\u{1F1F0} ") + String(h).replace(/\s+/g, " ").trim());
      }
    });
    let n = 0;
    for (const raw of R.rows) {
      const r = new Array(main.header.length).fill("");
      raw.forEach((v, j) => {
        const t = M.map[j] != null ? M.map[j] : extraIdx[j];
        if (t != null && (r[t] === "" || r[t] == null)) r[t] = v;
      });
      const k = svKey(r);
      if (seen.has(k)) continue;
      seen.add(k);
      r._src = x.label || "SK";
      r._lang = x.lang || "";
      main.rows.push(r);
      n++;
    }
    main.tabs.push({ title: (x.label || "arkusz") + ": " + R.tabs.map((t) => t.title).join(", "), rows: n, extra: true });
  }
  try {
    const id = await svSheetId(env);
    const v = await svGoogle(env, encodeURIComponent(id) + "/values/" + encodeURIComponent("'" + SVF_TEST_TAB + "'!A1:ZZ2000"));
    if (v.ok) {
      const vals = v.data.values || [], th = (vals[0] || []).map((h) => String(h || "").replace(/\s+/g, " ").trim());
      const baseLen = main.header.length, extraIdx = {};
      th.forEach((h, j) => {
        if (j < baseLen || !h) return;
        let k = main.header.indexOf(h);
        if (k < 0) {
          main.header.push(h);
          k = main.header.length - 1;
        }
        extraIdx[j] = k;
      });
      let n = 0;
      for (const raw of vals.slice(1)) {
        if (!(raw && raw.length > 2 && String(raw[1] || "").trim())) continue;
        const r = new Array(main.header.length).fill("");
        raw.forEach((x, j) => {
          const tg = j < baseLen ? j : extraIdx[j];
          if (tg != null) r[tg] = x;
        });
        const k = svKey(r);
        if (seen.has(k)) continue;
        seen.add(k);
        r._src = "\u{1F9EA} TEST";
        r._test = true;
        main.rows.push(r);
        n++;
      }
      if (n) main.tabs.push({ title: SVF_TEST_TAB, rows: n, extra: true });
    }
  } catch (e) {
  }
  main.extra_errors = errors;
  return main;
}
__name(svRows, "svRows");
__name2(svRows, "svRows");
async function svHeaderHash(h) {
  const d = await crypto.subtle.digest("SHA-256", new TextEncoder().encode((h || []).join("")));
  return [...new Uint8Array(d)].slice(0, 8).map((b) => b.toString(16).padStart(2, "0")).join("");
}
__name(svHeaderHash, "svHeaderHash");
async function svColMap(env, x, skH, plH) {
  skH = skH || [];
  plH = plH || [];
  const key = "svmap_" + x.id, hash = await svHeaderHash(skH);
  let saved = null;
  try {
    saved = JSON.parse(await appSettingGet(env, key) || "null");
  } catch (e) {
  }
  if (saved && saved.map && (saved.hash === hash || saved.manual)) return saved;
  const map = {};
  if (Math.abs(skH.length - plH.length) <= 1 && skH.every((h, j) => j < 2 || String(h || "").trim().toLowerCase() === String(plH[j] || "").trim().toLowerCase())) {
    skH.forEach((h, j) => {
      map[j] = j;
    });
  } else {
    map[0] = 0;
    const em = skH.findIndex((h) => /mail/i.test(String(h || "")));
    if (em >= 0) map[em] = 1;
    const L = /* @__PURE__ */ __name((a) => a.map((h, j) => j + ": " + String(h || "").replace(/\s+/g, " ").trim().slice(0, 160)).join("\n"), "L");
    const sys = "Dopasowujesz kolumny dw\xF3ch formularzy ankiety szko\u0142y Nowa Mowa (j\u0105kanie, bloki mowy): polskiego (wzorzec) i jego wersji " + (x.lang === "cs" ? "czeskiej" : "s\u0142owackiej") + ". Dla ka\u017Cdej kolumny formularza " + (x.lang === "cs" ? "czeskiego" : "s\u0142owackiego") + ' podaj numer kolumny polskiej, kt\xF3ra pyta o TO SAMO (np. ten sam objaw w nawiasie kwadratowym, ta sama osoba, ta sama informacja). Je\u015Bli nie ma odpowiednika \u2014 null. Jedna kolumna polska mo\u017Ce by\u0107 u\u017Cyta tylko raz. Uwa\u017Caj na pytania o dziecko i o rodzica, o wiek i dat\u0119 urodzenia, o kraj i miejscowo\u015B\u0107. Odpowiedz WY\u0141\u0104CZNIE obiektem JSON {"numer_kolumny_' + (x.lang === "cs" ? "cz" : "sk") + '": numer_kolumny_pl_lub_null, ...} dla wszystkich kolumn.';
    const user = "POLSKI (wzorzec):\n" + L(plH) + "\n\n" + (x.lang === "cs" ? "CZESKI" : "S\u0141OWACKI") + ":\n" + L(skH);
    try {
      const txt = await txLlm(env, sys, user, 4e3);
      const a = txt.indexOf("{"), z = txt.lastIndexOf("}");
      const o = a >= 0 && z > a ? JSON.parse(txt.slice(a, z + 1)) : null;
      if (o) {
        const used = new Set(Object.values(map));
        for (const [k, v] of Object.entries(o)) {
          const j = +k, p = v == null ? null : +v;
          if (!(j >= 0 && j < skH.length) || map[j] != null) continue;
          if (p != null && p >= 0 && p < plH.length && !used.has(p)) {
            map[j] = p;
            used.add(p);
          }
        }
      }
    } catch (e) {
    }
  }
  const out = { hash, map, manual: false, at: Math.floor(Date.now() / 1e3), sk_cols: skH.length, mapped: Object.keys(map).length };
  if (Object.keys(map).length > 2) {
    try {
      await appSettingSet(env, key, JSON.stringify(out));
    } catch (e) {
    }
  }
  return out;
}
__name(svColMap, "svColMap");
__name2(svColMap, "svColMap");
var SVF_TEST_TAB = "Ankieta online (test)";
var SVF_FREQ = ["nigdy", "rzadko", "czasami", "cz\u0119sto", "zawsze"];
var SVF_SIT = ["Rozmowa telefoniczna", "Zakupy w sklepie", "Pytanie przechodnia o drog\u0119", "Przedstawienie si\u0119 (imi\u0119 i nazwisko)", "Urz\u0105d, lekarz, bank", "Szko\u0142a albo praca (odpowied\u017A, spotkanie)", "Rozmowa w grupie", "Czytanie na g\u0142os", "Rozmowa z kim\u015B wa\u017Cnym (szef, nauczyciel)"];
var SVF_SIT_KID = ["Odpowied\u017A na lekcji", "Na przerwie z kolegami", "W domu z rodzin\u0105", "Czytanie na g\u0142os", "Rozmowa z nauczycielem", "Rozmowa telefoniczna", "Zakupy w sklepie", "Przedstawienie si\u0119 (imi\u0119 i nazwisko)", "Pytanie o drog\u0119"];
var SVF_SIT_OPT = ["0 \u2013 bez trudno\u015Bci", "1 \u2013 ma\u0142a", "2 \u2013 \u015Brednia", "3 \u2013 du\u017Ca", "4 \u2013 bardzo du\u017Ca / unikam", "nie dotyczy"];
var SVF_PR = ["rodzic"];
var SVF_SELF = ["uczen", "student", "praca", "inna"];
var SVF_SCHOOLKID = { role: ["rodzic"], stage: ["podstawowa", "srednia"] };
var SVF_COUNTRY_TOP = ["Polska", "S\u0142owacja", "Czechy", "Wielka Brytania"];
var SVF_COUNTRIES = { al: "Albania", ar: "Argentyna", am: "Armenia", au: "Australia", at: "Austria", az: "Azerbejd\u017Can", be: "Belgia", by: "Bia\u0142oru\u015B", ba: "Bo\u015Bnia i Hercegowina", br: "Brazylia", bg: "Bu\u0142garia", cn: "Chiny", hr: "Chorwacja", cy: "Cypr", me: "Czarnog\xF3ra", dk: "Dania", eg: "Egipt", ee: "Estonia", ph: "Filipiny", fi: "Finlandia", fr: "Francja", gr: "Grecja", ge: "Gruzja", es: "Hiszpania", nl: "Holandia", in: "Indie", ie: "Irlandia", is: "Islandia", il: "Izrael", jp: "Japonia", ca: "Kanada", kz: "Kazachstan", kr: "Korea Po\u0142udniowa", xk: "Kosowo", lt: "Litwa", lu: "Luksemburg", lv: "\u0141otwa", mk: "Macedonia P\xF3\u0142nocna", mt: "Malta", ma: "Maroko", mx: "Meksyk", md: "Mo\u0142dawia", de: "Niemcy", ng: "Nigeria", no: "Norwegia", nz: "Nowa Zelandia", pt: "Portugalia", za: "RPA", ru: "Rosja", ro: "Rumunia", rs: "Serbia", si: "S\u0142owenia", us: "Stany Zjednoczone", ch: "Szwajcaria", se: "Szwecja", th: "Tajlandia", tr: "Turcja", ua: "Ukraina", hu: "W\u0119gry", vn: "Wietnam", it: "W\u0142ochy", ae: "Zjednoczone Emiraty Arabskie", pl: "Polska", sk: "S\u0142owacja", cz: "Czechy", gb: "Wielka Brytania" };
var SVF_MONTHS = ["stycze\u0144", "luty", "marzec", "kwiecie\u0144", "maj", "czerwiec", "lipiec", "sierpie\u0144", "wrzesie\u0144", "pa\u017Adziernik", "listopad", "grudzie\u0144"];
var SVF_TR_FIX = { en: { "0 \u2013 bez trudno\u015Bci": "0 \u2013 not hard at all", "1 \u2013 ma\u0142a": "1 \u2013 a little hard", "2 \u2013 \u015Brednia": "2 \u2013 moderately hard", "3 \u2013 du\u017Ca": "3 \u2013 hard", "4 \u2013 bardzo du\u017Ca / unikam": "4 \u2013 very hard / I avoid it", "Wielka Brytania": "United Kingdom" } };
var SVF_FREQ_OPT = ["Codziennie", "2\u20133 razy w tygodniu", "Raz w tygodniu", "Rzadziej"];
var SVF_HIDE = /tony|do-re-mi|dynamizacj|kreatywno|rozwiń temat zainteres|uzupełnia opiekun|znajomość języka obcego|^data urodzenia\s*$|^kraj zamieszkania\s*$|^zainteresowania dziecka\s*$/i;
function svfHelp(k, q, col, colNot, hdr) {
  const S = { ans: {} };
  S.ans[k] = ["Tak"];
  return [
    { k, type: "chips", req: true, q, opts: ["Tak", "Nie"], col, colNot, hdr },
    { k: k + "Years", type: "text", show: S, q: "W jakich latach i jak d\u0142ugo?", hint: "np. 2015\u20132017, 2 lata", col: "w jakich latach", after: k, hdr: hdr + " \u2014 lata" },
    { k: k + "Freq", type: "chips", show: S, q: "Jak cz\u0119sto?", opts: SVF_FREQ_OPT, other: true, col: "jak cz\u0119sto", after: k, hdr: hdr + " \u2014 jak cz\u0119sto" },
    { k: k + "Who", type: "text", show: S, q: "Imi\u0119 i nazwisko specjalisty albo miejsce", col: "nazwisko|specjalist|miejsce pracy", after: k, hdr: hdr + " \u2014 specjalista" },
    { k: k + "Sat", type: "scale", show: S, q: "Na ile pomog\u0142o?", min: 1, max: 5, lo: "wcale", hi: "bardzo", col: "zadowolenie", after: k, hdr: hdr + " \u2014 zadowolenie (1\u20135)" }
  ];
}
__name(svfHelp, "svfHelp");
__name2(svfHelp, "svfHelp");
var SVF_STEPS = [
  { t: { a: "\u{1F44B} Zaczynamy", p: "\u{1F44B} Zaczynamy" }, note: { a: "Dziecko do 15 lat \u2014 ankiet\u0119 wype\u0142nia rodzic razem z dzieckiem. Starsza m\u0142odzie\u017C i doro\u015Bli \u2014 samodzielnie." }, q: [
    { k: "role", type: "cards", req: true, q: "Kto wype\u0142nia ankiet\u0119?", opts: [["rodzic", "\u{1F468}\u200D\u{1F469}\u200D\u{1F467} Rodzic lub opiekun", "razem z dzieckiem (do 15 lat)"], ["uczen", "\u{1F392} Ucze\u0144 lub student", "szko\u0142a albo studia (mo\u017Ce te\u017C pracuj\u0119)"], ["praca", "\u{1F4BC} Osoba pracuj\u0105ca", "pracuj\u0119 zawodowo"], ["inna", "\u{1F642} Inna sytuacja", "np. szukam pracy, jestem na emeryturze"]], hdr: "Kto wype\u0142nia \u2014 szczeg\xF3\u0142y" },
    { k: "school", type: "chips", req: true, show: { role: ["uczen"] }, q: "Gdzie si\u0119 uczysz?", opts: ["Szko\u0142a podstawowa", "Liceum", "Technikum", "Szko\u0142a bran\u017Cowa", "Studia"], other: true, col: "wykszta\u0142cenie|praca|szko\u0142a", colNot: "nazwa|zaj\u0119cia|trudn|przedszkole" },
    { k: "stage", type: "cards", req: true, show: { role: SVF_PR }, q: "Na jakim etapie jest dziecko?", opts: [["przedszkole", "\u{1F9F8} Przedszkole"], ["podstawowa", "\u{1F4DA} Szko\u0142a podstawowa"], ["srednia", "\u{1F3EB} Szko\u0142a \u015Brednia"], ["dorosle", "\u{1F393} Studia lub praca"]], hdr: "Etap dziecka" }
  ] },
  { t: { a: "\u2709\uFE0F Dane kontaktowe", p: "\u2709\uFE0F Dane kontaktowe" }, q: [
    { k: "first", type: "text", req: true, half: true, show: { role: SVF_SELF }, q: "Imi\u0119", ac: "given-name" },
    { k: "last", type: "text", req: true, half: true, show: { role: SVF_SELF }, q: "Nazwisko", ac: "family-name" },
    { k: "pfirst", type: "text", req: true, half: true, show: { role: SVF_PR }, q: "Imi\u0119 rodzica / opiekuna", ac: "given-name" },
    { k: "plast", type: "text", req: true, half: true, show: { role: SVF_PR }, q: "Nazwisko rodzica / opiekuna", ac: "family-name" },
    { k: "cfirst", type: "text", req: true, half: true, show: { role: SVF_PR }, q: "Imi\u0119 dziecka" },
    { k: "clast", type: "text", half: true, show: { role: SVF_PR }, q: "Nazwisko dziecka", hint: "je\u015Bli inne ni\u017C rodzica" },
    { k: "birth", type: "date", req: true, show: { role: SVF_SELF }, q: "Data urodzenia", col: "^data urodzenia(?! dziecka)|^rok urodzenia(?! dziecka)", ageCol: "^wiek(?! dziecka)", hdr: "Data urodzenia" },
    { k: "cbirth", type: "date", req: true, show: { role: SVF_PR }, q: "Data urodzenia dziecka", col: "data urodzenia dziecka", ageCol: "wiek dziecka", hdr: "Data urodzenia dziecka" },
    { k: "email", type: "email", req: true, q: "Adres e-mail", ac: "email", colIdx: 1 },
    { k: "phone", type: "tel", req: true, q: { a: "Telefon", p: "Telefon do rodzica" }, hint: "trener zadzwoni, \u017Ceby porozmawia\u0107 o wynikach ankiety", ac: "tel", col: "^(numer |nr )?telefon(u)?( kontaktowy)?\\s*$|numer telefonu", colNot: "rodzic", colP: "telefon do rodzica", hdr: "Telefon" },
    { k: "country", type: "country", q: "Kraj zamieszkania", def: "Polska", col: "^kraj|pa\u0144stw", colP: "kraj zamieszkania", hdr: "Kraj" },
    { k: "zip", type: "zip", half: true, show: { ans: { country: ["Polska"] } }, q: "Kod pocztowy", ac: "postal-code", hdr: "Kod pocztowy" },
    { k: "city", type: "text", req: true, half: true, q: "Miejscowo\u015B\u0107 zamieszkania", ac: "address-level2", col: "miejscowo|miasto|miejsce zamieszkania", colNot: "kraj|pa\u0144stw|\\(zamieszkanie\\)", colP: "miejscowo.*\\(?zamieszk", hdr: "Miejscowo\u015B\u0107" }
  ] },
  { t: { a: "\u{1F5E3} Twoja mowa", p: "\u{1F5E3} Mowa dziecka" }, q: [
    { k: "since", type: "chips", req: true, q: { a: "Od kiedy pojawiaj\u0105 si\u0119 trudno\u015Bci w mowie?", p: "Od kiedy u dziecka pojawiaj\u0105 si\u0119 trudno\u015Bci w mowie?" }, opts: ["Od wczesnego dzieci\u0144stwa", "Od czas\xF3w szkolnych", "Od kilku lat", "Od niedawna", "Nie wiem"], other: true, col: "czas trwania|od kiedy" },
    { k: "scale", type: "scale", req: true, q: { a: "Jak du\u017Cy jest dla Ciebie ten problem?", p: "Jak du\u017Cy jest ten problem dla dziecka?" }, min: 1, max: 5, lo: "niewielki", hi: "bardzo du\u017Cy", col: "skala problemu" },
    { k: "aware", type: "chips", req: true, show: { role: SVF_PR }, q: "Czy dziecko jest \u015Bwiadome trudno\u015Bci w mowie (j\u0105kania, blok\xF3w)?", opts: ["Tak, jest \u015Bwiadome", "Nie jest \u015Bwiadome", "Trudno powiedzie\u0107"], col: "\u015Bwiadomo\u015B\u0107", hdr: "\u015Awiadomo\u015B\u0107 problemu" },
    { k: "cause", type: "multi", q: { a: "Z czym wi\u0105\u017Cesz pocz\u0105tek trudno\u015Bci?", p: "Z czym wi\u0105\u017Ce Pani/Pan pocz\u0105tek trudno\u015Bci?" }, opts: ["Nie wiem", "Stres lub trudne wydarzenie", "Choroba lub uraz", "Zmiana otoczenia (przeprowadzka, nowa szko\u0142a)", "Kto\u015B w rodzinie te\u017C si\u0119 j\u0105ka", "Przestawianie r\u0119ki (lewor\u0119czno\u015B\u0107)"], other: true, col: "przyczyn" },
    { k: "family", type: "chips", q: "Czy kto\u015B w rodzinie si\u0119 j\u0105ka lub j\u0105ka\u0142?", opts: ["Tak", "Nie", "Nie wiem"], col: "rodziny.*(j\u0105ka|blok)|w rodzinie", hdr: "J\u0105kanie w rodzinie" },
    { k: "familyWho", type: "text", show: { ans: { family: ["Tak"] } }, q: "Kto?", hdr: "J\u0105kanie w rodzinie \u2014 kto" }
  ] },
  { t: { a: "\u{1FA7A} Objawy", p: "\u{1FA7A} Objawy u dziecka" }, grids: true, note: { a: "Zaznacz, jak cz\u0119sto to si\u0119 u Ciebie zdarza \u2014 w ka\u017Cdym wierszu. Nie ma z\u0142ych odpowiedzi.", p: "Zaznacz, jak cz\u0119sto to si\u0119 zdarza u dziecka \u2014 w ka\u017Cdym wierszu. Je\u015Bli nie wiesz, zaznacz \u201Enie wiem\u201D." } },
  { t: { a: "\u{1F5FA} Sytuacje", p: "\u{1F5FA} Sytuacje" }, q: [
    { k: "sit", type: "grid", req: true, q: { a: "Jak trudne s\u0105 dla Ciebie te sytuacje?", p: "Jak trudne s\u0105 te sytuacje dla dziecka?" }, hq: "Jak trudne s\u0105 dla Ciebie te sytuacje?", rows: SVF_SIT, rowsKid: SVF_SIT_KID, opts: SVF_SIT_OPT, hint: "Po kursie zapytamy o to samo \u2014 zobaczysz zmian\u0119." },
    {
      k: "harder",
      type: "multi",
      req: true,
      q: { a: "Kiedy m\xF3wi Ci si\u0119 TRUDNIEJ?", p: "Kiedy dziecku m\xF3wi si\u0119 TRUDNIEJ?" },
      opts: ["Przez telefon", "Z nowymi osobami", "W sklepie, urz\u0119dzie", "Przy przedstawianiu si\u0119", "W grupie", "Na spotkaniu w pracy lub na zaj\u0119ciach", "W po\u015Bpiechu lub zdenerwowaniu", "Przy zm\u0119czeniu", "Z osobami wa\u017Cnymi (szef, wyk\u0142adowca)"],
      optsKid: ["Przy odpowiedzi na lekcji", "Na przerwie z kolegami", "Przy czytaniu na g\u0142os", "Z nauczycielem", "Z nowymi osobami", "Przez telefon", "W sklepie", "Przy przedstawianiu si\u0119", "W po\u015Bpiechu lub zdenerwowaniu", "Przy zm\u0119czeniu", "W domu"],
      other: true,
      col: "CZ\u0118STO!|cz\u0119sto!|wyst\u0119puj\u0105 cz\u0119sto"
    },
    {
      k: "easier",
      type: "multi",
      q: { a: "Kiedy m\xF3wi Ci si\u0119 \u0141ATWIEJ?", p: "Kiedy dziecku m\xF3wi si\u0119 \u0141ATWIEJ?" },
      opts: ["W domu, z bliskimi", "Z przyjaci\xF3\u0142mi", "Gdy jestem sam(a) albo do zwierz\u0119cia", "Przy \u015Bpiewaniu", "Przy czytaniu razem z kim\u015B", "W spokoju", "Przy wolniejszym m\xF3wieniu"],
      optsKid: ["W domu, z bliskimi", "Z przyjaci\xF3\u0142mi", "Podczas zabawy", "Do zwierz\u0119cia albo zabawki", "Przy \u015Bpiewaniu", "Przy czytaniu razem z kim\u015B", "W spokoju", "Przy wolniejszym m\xF3wieniu"],
      other: true,
      col: "RZADKO!|rzadko!|wyst\u0119puj\u0105 rzadko"
    },
    { k: "lastSit", type: "textarea", q: { a: "Opisz ostatni\u0105 sytuacj\u0119, w kt\xF3rej mowa Ci przeszkodzi\u0142a. Co czu\u0142e\u015B(-a\u015B), co zrobi\u0142e\u015B(-a\u015B)?", p: "Opisz ostatni\u0105 sytuacj\u0119, w kt\xF3rej mowa przeszkodzi\u0142a dziecku. Jak zareagowa\u0142o?" }, hq: "Ostatnia sytuacja, w kt\xF3rej mowa przeszkodzi\u0142a" }
  ] },
  { t: { a: "\u{1F3EB} Szko\u0142a i praca", p: "\u{1F3EB} Przedszkole i szko\u0142a" }, q: [
    { k: "schoolName", type: "text", show: { role: SVF_PR, stage: ["przedszkole", "podstawowa", "srednia"] }, q: "Przedszkole / szko\u0142a \u2014 nazwa i klasa", hint: "np. SP 3 w Miko\u0142owie, klasa 4", col: "przedszkole ?/ ?szko\u0142a", hdr: "Przedszkole / szko\u0142a" },
    { k: "pre", type: "multi", show: { role: SVF_PR, stage: ["przedszkole"] }, q: "Jak wygl\u0105da mowa dziecka w przedszkolu?", opts: ["M\xF3wi ch\u0119tnie", "M\xF3wi mniej ni\u017C w domu", "Unika m\xF3wienia przy grupie", "Panie zauwa\u017Cy\u0142y trudno\u015Bci", "Dzieci zwracaj\u0105 uwag\u0119 na mow\u0119", "Nie wiem"], other: true, hdr: "Przedszkole" },
    { k: "schoolType", type: "chips", show: { or: [{ role: ["uczen"] }, { role: SVF_PR, stage: ["przedszkole", "podstawowa", "srednia", "dorosle"] }] }, q: { a: "Szko\u0142a / uczelnia jest:", p: "Przedszkole / szko\u0142a dziecka jest:" }, opts: ["Pa\u0144stwowa (publiczna)", "Prywatna", "Spo\u0142eczna lub inna"], hdr: "Szko\u0142a \u2014 pa\u0144stwowa / prywatna" },
    { k: "schoolSit", type: "multi", show: { or: [{ role: ["uczen"] }, SVF_SCHOOLKID] }, q: { a: "Co w szkole lub na uczelni jest najtrudniejsze?", p: "Co w szkole jest dla dziecka najtrudniejsze?" }, opts: ["Odpowied\u017A ustna", "Czytanie na g\u0142os", "Prezentacje i referaty", "Przedstawienie si\u0119 w nowej grupie", "Rozmowy z kolegami na przerwie", "Zg\u0142aszanie si\u0119 do odpowiedzi", "Egzaminy ustne"], other: true, hdr: "Szko\u0142a \u2014 trudne sytuacje" },
    { k: "teachers", type: "chips", show: { or: [{ kid: true, role: ["uczen"] }, SVF_SCHOOLKID] }, q: "Jak reaguj\u0105 nauczyciele?", opts: ["Wspieraj\u0105, daj\u0105 wi\u0119cej czasu", "Nie wiedz\u0105 o trudno\u015Bciach", "Nie zawsze rozumiej\u0105", "Trudno powiedzie\u0107"], hdr: "Szko\u0142a \u2014 nauczyciele" },
    { k: "peers", type: "chips", show: { or: [{ kid: true, role: ["uczen"] }, SVF_SCHOOLKID, { role: SVF_PR, stage: ["przedszkole"] }] }, q: "Jak jest z r\xF3wie\u015Bnikami?", opts: ["Bez problem\xF3w", "Czasem zdarzaj\u0105 si\u0119 docinki", "Zdarza si\u0119 dokuczanie", "Unikanie kontaktu", "Trudno powiedzie\u0107"], hdr: "R\xF3wie\u015Bnicy" },
    { k: "study", type: "text", show: { role: ["uczen"], ans: { school: ["Studia"] } }, q: "Kierunek studi\xF3w", col: "wykszta\u0142cenie|praca|szko\u0142a", colNot: "nazwa|zaj\u0119cia|trudn|przedszkole" },
    { k: "job", type: "text", show: { role: ["praca", "inna"] }, q: "Wykszta\u0142cenie i zaw\xF3d", hint: "np. technik informatyk, ksi\u0119gowa", col: "wykszta\u0142cenie|praca|szko\u0142a", colNot: "nazwa|zaj\u0119cia|trudn|przedszkole" },
    { k: "jobKid", type: "text", show: { role: SVF_PR, stage: ["dorosle"] }, q: "Czym zajmuje si\u0119 dziecko (studia, praca)?", col: "wykszta\u0142cenie|praca|szko\u0142a", colNot: "nazwa|zaj\u0119cia|trudn|przedszkole" },
    { k: "studentWork", type: "chips", show: { role: ["uczen"], ans: { school: ["Studia"] } }, q: "Czy pracujesz?", opts: ["Tak, na sta\u0142e", "Dorywczo", "Nie"], hdr: "Student \u2014 praca" },
    { k: "firm", type: "text", show: { or: [{ role: ["praca"] }, { role: ["uczen"], ans: { school: ["Studia"], studentWork: ["Tak, na sta\u0142e", "Dorywczo"] } }] }, q: "Gdzie pracujesz? (stanowisko, bran\u017Ca)", hint: "nazwa firmy nie jest konieczna", col: "nazwa aktualnej|firmy" },
    { k: "workCity", type: "text", show: { or: [{ role: ["praca"] }, { role: ["uczen"], ans: { school: ["Studia"], studentWork: ["Tak, na sta\u0142e", "Dorywczo"] } }] }, q: "Miejscowo\u015B\u0107, w kt\xF3rej pracujesz", hint: "je\u015Bli inna ni\u017C miejsce zamieszkania", hdr: "Miejscowo\u015B\u0107 pracy" },
    { k: "roles", type: "multi", show: { or: [{ role: ["praca"] }, { role: ["uczen"], ans: { school: ["Studia"], studentWork: ["Tak, na sta\u0142e", "Dorywczo"] } }] }, q: "Co robisz w pracy?", opts: ["Kontakt z klientem", "Telefony", "Spotkania i narady", "Prezentacje", "Kieruj\u0119 zespo\u0142em", "Praca g\u0142\xF3wnie bez rozm\xF3w"], other: true, col: "role w kt\xF3rych" },
    { k: "workImpact", type: "chips", show: { or: [{ role: ["praca", "inna"] }, { role: ["uczen"], ans: { school: ["Studia"], studentWork: ["Tak, na sta\u0142e", "Dorywczo"] } }] }, q: "Czy mowa wp\u0142ywa na Twoj\u0105 prac\u0119?", opts: ["Wcale", "Troch\u0119", "Mocno", "Przez mow\u0119 zmieni\u0142em(-am) lub nie podj\u0105\u0142em(-\u0119\u0142am) pracy"], hdr: "Wp\u0142yw na prac\u0119" },
    { k: "react", type: "multi", req: true, show: { role: SVF_PR }, q: "Jak dziecko reaguje, gdy mowa si\u0119 zacina?", opts: ["Spokojnie, m\xF3wi dalej", "Z\u0142o\u015Bci si\u0119", "P\u0142acze lub si\u0119 wycofuje", "Unika m\xF3wienia", "Zamienia s\u0142owa", "Prosi, \u017Ceby kto\u015B powiedzia\u0142 za nie", "Chyba nie zauwa\u017Ca"], other: true, hdr: "Reakcja dziecka na trudno\u015Bci" },
    { k: "talks", type: "chips", show: { role: SVF_PR }, q: "Czy dziecko rozmawia z Pani\u0105/Panem o swojej mowie?", opts: ["Tak, otwarcie", "Czasem", "Nie, unika tematu"], hdr: "Dziecko m\xF3wi o swojej mowie" }
  ] },
  { t: { a: "\u{1FA79} Wcze\u015Bniejsza pomoc", p: "\u{1FA79} Wcze\u015Bniejsza pomoc" }, q: [].concat(
    svfHelp("logo", { a: "Czy chodzi\u0142e\u015B(-a\u015B) do logopedy?", p: "Czy dziecko chodzi\u0142o do logopedy?" }, "logoped", "lat|zadowol|nazwisk|cz\u0119sto", "Logopeda"),
    svfHelp("psych", { a: "Czy korzysta\u0142e\u015B(-a\u015B) z pomocy psychologa?", p: "Czy dziecko korzysta\u0142o z pomocy psychologa?" }, "psycholog", "lat|zadowol|nazwisk|cz\u0119sto", "Psycholog"),
    svfHelp("otherH", { a: "Czy korzysta\u0142e\u015B(-a\u015B) z innych form pomocy (kursy, zaj\u0119cia, aplikacje)?", p: "Czy dziecko korzysta\u0142o z innych form pomocy (kursy, zaj\u0119cia, aplikacje)?" }, "inne (formy|zaj\u0119cia|terap)", "lat|zadowol|nazwisk|cz\u0119sto", "Inna pomoc"),
    [
      { k: "otherWhat", type: "text", show: { ans: { otherH: ["Tak"] } }, q: "Jakie? Gdzie?", col: "w jakich latach i gdzie|inne (formy|zaj\u0119cia)", hdr: "Inna pomoc \u2014 jaka" },
      { k: "otherExp", type: "textarea", q: "Co pomaga\u0142o, a co nie? Czy efekty si\u0119 utrzyma\u0142y?", col: "do\u015Bwiadczenia" }
    ]
  ) },
  { t: { a: "\u{1F3E0} Ty i Twoje otoczenie", p: "\u{1F3E0} Dziecko i otoczenie" }, q: [
    { k: "live", type: "chips", q: { a: "Z kim mieszkasz?", p: "Z kim mieszka dziecko?" }, opts: ["Z partnerem / partnerk\u0105", "Z rodzicami", "Sam(a)", "Ze znajomymi"], optsKid: ["Z obojgiem rodzic\xF3w", "Z jednym rodzicem", "Na zmian\u0119 u ka\u017Cdego z rodzic\xF3w", "Z rodzicami i dziadkami", "Z dziadkami"], other: true, col: "z kim mieszka", colNot: "dziecko", colP: "z kim mieszka dziecko" },
    { k: "alive", type: "chips", show: { kid: true }, q: { a: "Czy oboje Twoi rodzice \u017Cyj\u0105?", p: "Czy oboje rodzice dziecka \u017Cyj\u0105?" }, opts: ["Tak, oboje", "Jedno z rodzic\xF3w nie \u017Cyje", "Oboje nie \u017Cyj\u0105"], hdr: "Rodzice \u017Cyj\u0105" },
    { k: "divorce", type: "chips", show: { kid: true }, q: { a: "Czy Twoi rodzice s\u0105 po rozwodzie lub rozstaniu?", p: "Czy rodzice dziecka s\u0105 po rozwodzie lub rozstaniu?" }, opts: ["Nie", "Tak, po rozwodzie", "Tak, po rozstaniu", "Wol\u0119 nie odpowiada\u0107"], hdr: "Rozw\xF3d / rozstanie rodzic\xF3w" },
    { k: "divorceWhen", type: "text", show: { ans: { divorce: ["Tak, po rozwodzie", "Tak, po rozstaniu"] } }, q: "Ile lat temu? (albo rok i ile lat mia\u0142o wtedy dziecko)", hdr: "Rozw\xF3d / rozstanie \u2014 kiedy" },
    { k: "parentEdu", type: "multi", show: { role: SVF_PR }, q: "Czy ma Pani/Pan wykszta\u0142cenie:", opts: ["Pedagogiczne", "Psychologiczne", "Medyczne", "\u017Badne z tych"], other: true, hdr: "Wykszta\u0142cenie rodzica" },
    { k: "siblings", type: "text", show: { kid: true }, q: { a: "Rodze\u0144stwo \u2014 p\u0142e\u0107 i wiek", p: "Rodze\u0144stwo dziecka \u2014 p\u0142e\u0107 i wiek" }, hint: "np. siostra 8 lat, brat 3 lata; wpisz \u201Ebrak\u201D, je\u015Bli nie ma", col: "rodze\u0144stw", hdr: "Rodze\u0144stwo" },
    { k: "kids", type: "chips", show: { or: [{ role: ["praca", "inna"] }, { role: ["uczen"], ans: { school: ["Studia"] } }] }, q: "Czy masz dzieci?", opts: ["Nie", "Tak"], hdr: "Ma dzieci" },
    { k: "kidsAge", type: "text", show: { or: [{ role: ["praca", "inna"], ans: { kids: ["Tak"] } }, { role: ["uczen"], ans: { school: ["Studia"], kids: ["Tak"] } }] }, q: "W jakim wieku s\u0105 Twoje dzieci?", hint: "np. 4 i 9 lat", hdr: "Dzieci \u2014 wiek" },
    { k: "raise", type: "chips", show: { role: SVF_PR }, q: "Dziecko wychowuje Pani/Pan:", opts: ["Sam(a)", "Z ma\u0142\u017Conkiem / partnerem", "Na zmian\u0119 z drugim rodzicem", "Z pomoc\u0105 dziadk\xF3w"], other: true, hdr: "Wychowanie dziecka" },
    { k: "pPsych", type: "chips", show: { role: SVF_PR }, q: "Czy ma Pani/Pan zdiagnozowane trudno\u015Bci lub choroby natury psychologicznej (np. l\u0119ki, depresja)?", opts: ["Nie", "Tak", "Wol\u0119 nie odpowiada\u0107"], hdr: "Rodzic \u2014 trudno\u015Bci psychologiczne" },
    { k: "pPsychWhat", type: "text", show: { role: SVF_PR, ans: { pPsych: ["Tak"] } }, q: "Jakie? (opcjonalnie)", hdr: "Rodzic \u2014 trudno\u015Bci psychologiczne \u2014 jakie" },
    { k: "close", type: "multi", q: { a: "Jak bliscy reaguj\u0105 na Twoje trudno\u015Bci w mowie?", p: "Jak rodzina reaguje na trudno\u015Bci dziecka?" }, opts: ["Wspieraj\u0105", "Nie zwracaj\u0105 uwagi", "Bagatelizuj\u0105", "Denerwuj\u0105 si\u0119 lub poganiaj\u0105", "Ko\u0144cz\u0105 zdania", "Nie wiem"], other: true, col: "bliscy postrzegaj\u0105|postrzegaj\u0105" },
    { k: "who", type: "multi", q: { a: "Kto wie o Twoich trudno\u015Bciach?", p: "Kto wie o trudno\u015Bciach dziecka?" }, opts: ["Rodzina", "Partner / partnerka", "Przyjaciele", "Szko\u0142a lub praca", "Nikt"], optsKid: ["Rodzina", "Przyjaciele", "Nauczyciele", "Koledzy z klasy", "Nikt"], col: "kto wie o" },
    { k: "hobby", type: "multi", q: { a: "Twoje zainteresowania", p: "Zainteresowania dziecka" }, opts: ["Sport", "Muzyka", "Gry", "Ksi\u0105\u017Cki", "Taniec", "Rysunek, sztuka", "Technika, komputery", "Przyroda, zwierz\u0119ta"], other: true, col: "zainteres|hobby", colNot: "dziecka|rozwi\u0144", colP: "zainteresowania dziecka" },
    { k: "sportWhat", type: "text", show: { ans: { hobby: ["Sport"] } }, q: "Jaki sport?", hint: "np. pi\u0142ka no\u017Cna, p\u0142ywanie", hdr: "Sport \u2014 jaki" },
    { k: "sportHow", type: "chips", show: { ans: { hobby: ["Sport"] } }, q: "Sport \u2014 jak?", opts: ["Tylko ogl\u0105dam", "Rekreacyjnie", "Trenuj\u0119 w klubie", "Startuj\u0119 w zawodach"], hdr: "Sport \u2014 jak" },
    { k: "musicHow", type: "multi", show: { ans: { hobby: ["Muzyka"] } }, q: "Muzyka \u2014 jak?", opts: ["S\u0142ucham", "\u015Apiewam", "Gram na instrumencie", "W zespole lub ch\xF3rze", "Szko\u0142a muzyczna"], hdr: "Muzyka \u2014 jak" },
    { k: "musicGenre", type: "text", show: { ans: { hobby: ["Muzyka"] } }, q: "Jaka muzyka?", hdr: "Muzyka \u2014 jaka" },
    { k: "music", type: "chips", q: { a: "Czy grasz na instrumencie?", p: "Czy dziecko gra na instrumencie?" }, opts: ["Tak", "Nie"], col: "instrumen|gra\u0107 na|gra na", colNot: "dziecko", colP: "dziecko.*(gra\u0107|instrumen)", hdr: "Instrument" },
    { k: "musicWhat", type: "text", show: { ans: { music: ["Tak"] } }, q: "Na jakim instrumencie?", hdr: "Instrument \u2014 jaki" },
    { k: "sing", type: "chips", q: { a: "Czy \u015Bpiewasz?", p: "Czy dziecko \u015Bpiewa?" }, opts: ["Tak", "Nie"], col: "potrafisz \u015Bpiewa\u0107|\u015Bpiewasz", colNot: "dziecko", colP: "dziecko.*\u015Bpiew", hdr: "\u015Apiew" },
    { k: "lang", type: "multi", q: { a: "Jakich j\u0119zyk\xF3w obcych si\u0119 uczysz lub znasz?", p: "Jakich j\u0119zyk\xF3w obcych uczy si\u0119 dziecko?" }, opts: ["Angielski", "Niemiecki", "Hiszpa\u0144ski", "Francuski", "\u017Baden"], other: true, col: "j\u0119zyka obcego" },
    { k: "ill", type: "chips", q: { a: "Czy masz zdiagnozowane choroby lub inne dolegliwo\u015Bci, o kt\xF3rych trener powinien wiedzie\u0107?", p: "Czy dziecko ma zdiagnozowane choroby lub inne dolegliwo\u015Bci, o kt\xF3rych trener powinien wiedzie\u0107?" }, opts: ["Nie", "Tak"], col: "przewlek\u0142e|choroby|dolegliwo", colNot: "zdiagnozowane|jakie" },
    { k: "illWhat", type: "text", show: { ans: { ill: ["Tak"] } }, q: "Jakie?", col: "zdiagnozowane|jakie choroby", hdr: "Choroby \u2014 jakie" },
    { k: "meds", type: "chips", q: { a: "Czy przyjmujesz leki?", p: "Czy dziecko przyjmuje leki?" }, opts: ["Nie", "Tak"], col: "przyjmujesz leki|leki", hdr: "Leki" },
    { k: "medsWhat", type: "text", show: { ans: { meds: ["Tak"] } }, q: "Jakie?", hdr: "Leki \u2014 jakie" }
  ] },
  { t: { a: "\u{1F3AF} Cel i kurs", p: "\u{1F3AF} Cel i kurs" }, q: [
    {
      k: "goal",
      type: "multi",
      req: true,
      q: { a: "Czego oczekujesz po kursie?", p: "Czego oczekuje Pani/Pan po kursie?" },
      opts: ["Swobodne rozmowy przez telefon", "Przedstawianie si\u0119 bez stresu", "Pewno\u015B\u0107 siebie", "Wyst\u0105pienia i prezentacje", "Rozmowy w pracy i z klientami", "Mniej unikania sytuacji"],
      optsKid: ["Swobodne odpowiedzi w szkole", "Swobodne rozmowy z kolegami", "Czytanie na g\u0142os bez stresu", "Przedstawianie si\u0119 bez stresu", "Pewno\u015B\u0107 siebie", "Rozmowy przez telefon", "Mniej unikania sytuacji"],
      other: true,
      col: "oczekujesz|oczekiwania|cel kursu"
    },
    { k: "impact", type: "grid", q: { a: "Na ile mowa Ci\u0119 ogranicza?", p: "Na ile mowa ogranicza dziecko?" }, hq: "Na ile mowa Ci\u0119 ogranicza?", rows: ["w szkole lub pracy", "w relacjach z lud\u017Ami", "w codziennych sprawach", "w planach i decyzjach"], rowsKid: ["w szkole / przedszkolu", "w relacjach z r\xF3wie\u015Bnikami", "w codziennych sprawach", "w zabawie i zaj\u0119ciach dodatkowych"], opts: ["0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10"], hint: "0 = wcale, 10 = bardzo" },
    { k: "form", type: "chips", req: true, q: "Jaka forma kursu odpowiada?", opts: ["Na miejscu w Miko\u0142owie", "Online", "Ka\u017Cda mi odpowiada", "Jeszcze nie wiem"], hdr: "Forma kursu" },
    { k: "when", type: "chips", req: true, q: "Kiedy chcesz zacz\u0105\u0107?", opts: ["Jak najszybciej", "W ci\u0105gu 3 miesi\u0119cy", "P\xF3\u017Aniej", "Jeszcze nie wiem"], hdr: "Kiedy zacz\u0105\u0107" },
    { k: "travel", type: "chips", q: "Czy przyjazd do Miko\u0142owa na czas kursu jest mo\u017Cliwy?", opts: ["Tak", "Trudno, ale si\u0119 da", "Nie"], hdr: "Dojazd do Miko\u0142owa" },
    { k: "time", type: "chips", q: { a: "Ile czasu dziennie mo\u017Cesz przeznaczy\u0107 na trening?", p: "Ile czasu dziennie dziecko mo\u017Ce przeznaczy\u0107 na trening?" }, opts: ["do 15 minut", "15\u201330 minut", "30\u201360 minut", "ponad godzin\u0119"], hdr: "Czas na trening dziennie" },
    { k: "ready", type: "scale", q: { a: "Na ile jeste\u015B got\xF3w(-owa) teraz co\u015B zmieni\u0107?", p: "Na ile dziecko jest gotowe teraz co\u015B zmieni\u0107?" }, min: 1, max: 10, lo: "wcale", hi: "bardzo", hdr: "Gotowo\u015B\u0107 do zmiany (1\u201310)" },
    { k: "fund", type: "check", q: "Chc\u0119 dosta\u0107 informacj\u0119 o dofinansowaniu kursu", hdr: "Informacja o dofinansowaniu" },
    { k: "source", type: "multi", q: "Sk\u0105d wiesz o Nowej Mowie?", opts: ["YouTube", "Facebook", "Instagram", "TikTok", "Google", "Polecenie znajomych", "Logopeda lub lekarz", "Telewizja, prasa"], other: true, col: "sk\u0105d dowiedzia" }
  ] },
  { t: { a: "\u{1F4CB} Jeszcze kilka pyta\u0144", p: "\u{1F4CB} Jeszcze kilka pyta\u0144" }, rest: true },
  { t: { a: "\u2705 Zgody i wysy\u0142ka", p: "\u2705 Zgody i wysy\u0142ka" }, q: [
    { k: "consent", type: "check", req: true, q: "Zgadzam si\u0119 na przetwarzanie danych z ankiety (tak\u017Ce o zdrowiu i mowie) przez Szko\u0142\u0119 Nowej Mowy w celu przygotowania kursu i kontaktu w jego sprawie.", hdr: "Zgoda: przetwarzanie danych" },
    { k: "consentMail", type: "check", q: "Chc\u0119 dostawa\u0107 e-maile od Szko\u0142y Nowej Mowy (informacje o kursie i terminach).", hdr: "Zgoda: e-maile" },
    { k: "consentResearch", type: "check", q: "Zgadzam si\u0119 na wykorzystanie moich odpowiedzi \u2014 bez imienia, nazwiska i danych kontaktowych \u2014 w badaniach naukowych nad p\u0142ynno\u015Bci\u0105 mowy.", hdr: "Zgoda: badania naukowe" }
  ] }
];
function svfAllQ() {
  const out = [];
  SVF_STEPS.forEach((s) => (s.q || []).forEach((q) => out.push(q)));
  return out;
}
__name(svfAllQ, "svfAllQ");
__name2(svfAllQ, "svfAllQ");
function svfOrder(opts) {
  const f = /* @__PURE__ */ __name((s) => SVF_FREQ.findIndex((w) => String(s).toLowerCase().startsWith(w)), "f");
  if (opts.every((o) => /^\d+([.,]\d+)?$/.test(String(o).trim()))) return opts.slice().sort((a, b) => parseFloat(a) - parseFloat(b));
  if (opts.every((o) => f(o) >= 0)) return opts.slice().sort((a, b) => f(a) - f(b));
  if (opts.every((o) => /^(tak|nie)\b/i.test(o))) return opts.slice().sort((a, b) => (/^tak/i.test(a) ? 0 : 1) - (/^tak/i.test(b) ? 0 : 1));
  return opts;
}
__name(svfOrder, "svfOrder");
__name2(svfOrder, "svfOrder");
async function svfCfg(env) {
  let c = {};
  try {
    c = JSON.parse(await appSettingGet(env, "svf_cfg") || "{}") || {};
  } catch (e) {
  }
  if (typeof c !== "object") c = {};
  if (c.public_base == null) c.public_base = "https://ask.nowamowa.com";
  return c;
}
__name(svfCfg, "svfCfg");
__name2(svfCfg, "svfCfg");
var SVF_LANGS = { pl: "polski", sk: "s\u0142owacki", cs: "czeski", en: "angielski" };
function svfYtThumb(u) {
  const m = String(u || "").match(/(?:youtu\.be\/|youtube\.com\/(?:watch\?(?:.*&)?v=|embed\/|shorts\/|live\/))([\w-]{11})/);
  return m ? "https://img.youtube.com/vi/" + m[1] + "/hqdefault.jpg" : "";
}
__name(svfYtThumb, "svfYtThumb");
function svfLinksFor(cfg, kid, lang) {
  const all = cfg.links || {}, flat = all.film_kids != null || all.film_adults != null || all.esbs != null;
  const L = (flat ? lang === "pl" || !lang ? all : {} : all[lang || "pl"] || {}) || {}, out = [];
  const film = kid ? L.film_kids || L.film_adults : L.film_adults || L.film_kids;
  if (film) out.push({ t: kid ? "\u{1F3AC} Film z telewizji \u2014 o dzieciach i j\u0105kaniu" : "\u{1F3AC} Film z telewizji o Nowej Mowie", u: film, th: svfYtThumb(film) });
  if (L.esbs) out.push({ t: "\u{1F393} Wyk\u0142ad o ESBS", u: L.esbs, th: svfYtThumb(L.esbs) });
  const shop = L.shop || ((lang || "pl") === "pl" ? "https://nowamowa.com/sklep/" : "");
  if (shop) out.push({ t: "\u{1F4DA} Ksi\u0105\u017Cki i materia\u0142y \u2014 sklep Nowej Mowy", u: shop, book: L.book_title || ((lang || "pl") === "pl" ? "Emocjonalne bloki mowy" : ""), img: L.book_img || "" });
  return out.filter((x) => /^https?:\/\//.test(x.u)).map((x) => {
    if (!x.book) delete x.book;
    if (!x.img) delete x.img;
    if (!x.th) delete x.th;
    return x;
  });
}
__name(svfLinksFor, "svfLinksFor");
__name2(svfLinksFor, "svfLinksFor");
var SVF_UI = [
  "Ankieta przed kursem",
  "Kwestionariusz pomocny przy rozpoznaniu j\u0105kania i blok\xF3w mowy ESBS",
  "ok. 10 minut \xB7 wi\u0119kszo\u015B\u0107 odpowiedzi to klikni\u0119cia \xB7 zapisuje si\u0119 na bie\u017C\u0105co",
  "\u2190 Wstecz",
  "Dalej \u2192",
  "Wy\u015Blij ankiet\u0119 \u2713",
  "Krok %1 z %2",
  "mo\u017Cesz zaznaczy\u0107 kilka",
  "Inne\u2026",
  "Wpisz\u2026",
  "Masz mniej ni\u017C 15 lat \u2014 popro\u015B rodzica, \u017Ceby wype\u0142ni\u0142 ankiet\u0119 razem z Tob\u0105.",
  "%1 lat",
  "Uzupe\u0142nij zaznaczone pola.",
  "\u23F3 Wysy\u0142am\u2026",
  "Dzi\u0119kujemy!",
  "Ankieta zosta\u0142a zapisana. Trener przeczyta odpowiedzi i odezwie si\u0119, \u017Ceby porozmawia\u0107.",
  "Zanim porozmawiamy \u2014 warto obejrze\u0107:",
  "Wys\u0142ali\u015Bmy te\u017C te linki na Tw\xF3j e-mail.",
  "Nie uda\u0142o si\u0119 wys\u0142a\u0107:",
  "Odpowiedzi s\u0105 zapami\u0119tane \u2014 spr\xF3buj za chwil\u0119.",
  "Te pytania s\u0105 z dotychczasowej ankiety.",
  "\u{1F3AC} Film z telewizji \u2014 o dzieciach i j\u0105kaniu",
  "\u{1F3AC} Film z telewizji o Nowej Mowie",
  "\u{1F393} Wyk\u0142ad o ESBS",
  "\u{1F4DA} Ksi\u0105\u017Cki i materia\u0142y \u2014 sklep Nowej Mowy",
  "\u{1F64F} Dzi\u0119kujemy za ankiet\u0119",
  "nie wiem",
  "Zaznacz odpowied\u017A w ka\u017Cdym wierszu.",
  "Dzie\u0144 dobry,",
  "Cze\u015B\u0107",
  "Dzie\u0144",
  "Miesi\u0105c",
  "Rok",
  "Inny kraj\u2026",
  "Wyszukaj kraj\u2026",
  "\u{1F4CD} U\u017Cyj mojej lokalizacji",
  "\u23F3 Szukam lokalizacji\u2026",
  "Nie uda\u0142o si\u0119 pobra\u0107 lokalizacji.",
  "\u25B6 Obejrzyj",
  "\u{1F4DA} Polecamy ksi\u0105\u017Ck\u0119",
  "Zobacz w sklepie",
  "Emocjonalne bloki mowy",
  "dzi\u0119kujemy za wype\u0142nienie ankiety. Trener przeczyta odpowiedzi i odezwie si\u0119, \u017Ceby porozmawia\u0107.",
  "Dzi\u0119kujemy za ankiet\u0119 \u2014 Nowa Mowa"
];
function svfStrings(D) {
  const out = new Set(SVF_UI), add = /* @__PURE__ */ __name((x) => {
    if (!x) return;
    if (typeof x === "string") {
      if (/\p{L}/u.test(x)) out.add(x);
    } else if (Array.isArray(x)) x.forEach(add);
    else if (typeof x === "object") {
      add(x.a);
      add(x.p);
    }
  }, "add");
  add(SVF_MONTHS);
  add(Object.values(SVF_COUNTRIES));
  SVF_STEPS.forEach((s) => {
    add(s.t);
    add(s.note);
    (s.q || []).forEach((q) => {
      add(q.q);
      add(q.hint);
      add(q.lo);
      add(q.hi);
      add(q.rows);
      add(q.rowsKid);
      (q.opts || []).concat(q.optsKid || []).forEach((o) => {
        if (Array.isArray(o)) {
          add(o[1]);
          add(o[2]);
        } else add(o);
      });
    });
  });
  (D.grids || []).forEach((g) => {
    add(g.q);
    g.rows.forEach((r) => add(r.r));
    add(g.opts);
  });
  (D.rest || []).forEach((q) => {
    add(q.q);
    add(q.opts);
  });
  return [...out];
}
__name(svfStrings, "svfStrings");
__name2(svfStrings, "svfStrings");
async function svfTr(env, lang, strings) {
  if (!lang || lang === "pl" || !SVF_LANGS[lang]) return {};
  await env.DB.prepare("CREATE TABLE IF NOT EXISTS svf_tr (h TEXT PRIMARY KEY, lang TEXT, src TEXT, out TEXT, at INTEGER)").run();
  const key = /* @__PURE__ */ __name(async (s) => {
    const d = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(lang + "|" + s));
    return [...new Uint8Array(d)].slice(0, 12).map((b) => b.toString(16).padStart(2, "0")).join("");
  }, "key");
  const map = {}, keys = {};
  for (const s of strings) keys[s] = await key(s);
  const ks = Object.values(keys);
  for (let i = 0; i < ks.length; i += 80) {
    const part = ks.slice(i, i + 80);
    const rs = (await env.DB.prepare("SELECT h, out FROM svf_tr WHERE h IN (" + part.map(() => "?").join(",") + ")").bind(...part).all()).results || [];
    const byH = {};
    rs.forEach((r) => {
      byH[r.h] = r.out;
    });
    strings.forEach((s) => {
      if (byH[keys[s]] != null) map[s] = byH[keys[s]];
    });
  }
  const todo = strings.filter((s) => map[s] == null);
  if (todo.length) {
    const L = SVF_LANGS[lang] === "angielski" ? "angielski (British/neutral English)" : SVF_LANGS[lang];
    const sys = "Jeste\u015B zawodowym t\u0142umaczem z polskiego na j\u0119zyk " + L + ". T\u0142umaczysz ankiet\u0119 przed kursem p\u0142ynnego m\xF3wienia (dla os\xF3b, kt\xF3re si\u0119 j\u0105kaj\u0105, i rodzic\xF3w dzieci) szko\u0142y \u201ESzko\u0142a Nowej Mowy\u201D (metoda Nowa Mowa\xAE). Dostajesz tablic\u0119 JSON z kr\xF3tkimi tekstami (pytania, odpowiedzi do wyboru, przyciski). Zwr\xF3\u0107 WY\u0141\u0104CZNIE tablic\u0119 JSON z t\u0142umaczeniami: tyle samo element\xF3w, ta sama kolejno\u015B\u0107. Zachowaj emoji, znaki %1 i %2, adresy URL i nazwy w\u0142asne (Nowa Mowa, Szko\u0142a Nowej Mowy, Miko\u0142\xF3w, ESBS). Formy \u201E-a\u015B\u201D, \u201E(a)\u201D, \u201E(-owa)\u201D zamie\u0144 na naturaln\u0105 form\u0119 w j\u0119zyku docelowym (neutraln\u0105 p\u0142ciowo, je\u015Bli si\u0119 da). Zwracaj si\u0119 do osoby wype\u0142niaj\u0105cej uprzejmie i ciep\u0142o (" + (lang === "en" ? "\u201Eyou\u201D" : "forma \u201EVy\u201D w pytaniach do rodzic\xF3w, \u201Ety\u201D do m\u0142odzie\u017Cy i doros\u0142ych tak jak w oryginale") + "). Nie u\u017Cywaj s\u0142\xF3w: terapia, terapeuta, terapeutyczny \u2014 zamiast tego trening, kurs, specjalista. Polskie nazwy szk\xF3\u0142 i dokument\xF3w dopasuj do reali\xF3w kraju tylko gdy to oczywiste.";
    const parts = [];
    for (let i = 0; i < todo.length; i += 30) parts.push(todo.slice(i, i + 30));
    const now = Math.floor(Date.now() / 1e3);
    for (let i = 0; i < parts.length; i += 6) {
      await Promise.all(parts.slice(i, i + 6).map(async (src) => {
        for (let attempt = 0; attempt < 2; attempt++) {
          try {
            const txt = await txLlm(env, sys, JSON.stringify(src), Math.min(6e3, 300 + src.join("").length * 3));
            const a = txt.indexOf("["), z = txt.lastIndexOf("]"), p = a >= 0 && z > a ? JSON.parse(txt.slice(a, z + 1)) : null;
            if (!Array.isArray(p) || p.length !== src.length) continue;
            for (let j = 0; j < src.length; j++) {
              const o = String(p[j] == null ? "" : p[j]).trim();
              if (!o) continue;
              map[src[j]] = o;
              try {
                await env.DB.prepare("INSERT OR REPLACE INTO svf_tr (h, lang, src, out, at) VALUES (?, ?, ?, ?, ?)").bind(keys[src[j]], lang, src[j], o, now).run();
              } catch (e) {
              }
            }
            return;
          } catch (e) {
          }
        }
      }));
    }
  }
  return map;
}
__name(svfTr, "svfTr");
__name2(svfTr, "svfTr");
async function svfTrAll(env, lang, strings, fastOnly) {
  if (!lang || lang === "pl" || !SVF_LANGS[lang]) return {};
  const d = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(strings.join(""))), h = [...new Uint8Array(d)].slice(0, 10).map((b) => b.toString(16).padStart(2, "0")).join("");
  const fix = /* @__PURE__ */ __name((m) => m ? Object.assign({}, m, SVF_TR_FIX[lang] || {}) : m, "fix");
  try {
    const c = JSON.parse(await appSettingGet(env, "svf_trmap_" + lang) || "null");
    if (c && c.h === h && c.map) return fix(c.map);
    if (fastOnly) return c && c.map ? fix(c.map) : null;
  } catch (e) {
    if (fastOnly) return null;
  }
  const map = await svfTr(env, lang, strings);
  if (strings.every((s) => map[s] != null)) {
    try {
      await appSettingSet(env, "svf_trmap_" + lang, JSON.stringify({ h, map }));
    } catch (e) {
    }
  }
  return fix(map);
}
__name(svfTrAll, "svfTrAll");
__name2(svfTrAll, "svfTrAll");
var SVF_FLAG = {
  pl: '<svg viewBox="0 0 30 20" width="38" height="26"><rect width="30" height="10" fill="#fff"/><rect y="10" width="30" height="10" fill="#DC143C"/></svg>',
  sk: '<svg viewBox="0 0 30 20" width="38" height="26"><rect width="30" height="6.67" fill="#fff"/><rect y="6.67" width="30" height="6.67" fill="#0B4EA2"/><rect y="13.33" width="30" height="6.67" fill="#EE1C25"/><path d="M7 4.5h8v7c0 2.6-2.3 4.1-4 4.9-1.7-.8-4-2.3-4-4.9z" fill="#EE1C25" stroke="#fff" stroke-width=".8"/><path d="M10.6 6v8.5h.8V6zm-1.8 2.2h4.4v.8H8.8zm-.6 2h5.6v.8H8.2z" fill="#fff"/><path d="M7.4 14.2c1 .3 1.6-.8 2.4-.8s1 .7 1.2.7.4-.7 1.2-.7 1.4 1.1 2.4.8c-.6 1-2 1.8-3.6 2.4-1.6-.6-3-1.4-3.6-2.4z" fill="#0B4EA2"/></svg>',
  cs: '<svg viewBox="0 0 30 20" width="38" height="26"><rect width="30" height="10" fill="#fff"/><rect y="10" width="30" height="10" fill="#D7141A"/><path d="M0 0l15 10L0 20z" fill="#11457E"/></svg>',
  en: '<svg viewBox="0 0 60 40" width="38" height="26"><clipPath id="u"><rect width="60" height="40"/></clipPath><g clip-path="url(#u)"><rect width="60" height="40" fill="#012169"/><path d="M0 0l60 40M60 0L0 40" stroke="#fff" stroke-width="8"/><path d="M0 0l60 40M60 0L0 40" stroke="#C8102E" stroke-width="3"/><path d="M30 0v40M0 20h60" stroke="#fff" stroke-width="12"/><path d="M30 0v40M0 20h60" stroke="#C8102E" stroke-width="7"/></g></svg>'
};
async function svfDef(env, fresh, ctx) {
  if (!fresh) {
    try {
      const c = JSON.parse(await appSettingGet(env, "svf_def3") || "null");
      if (c && c.header) {
        if (c.at > Date.now() / 1e3 - 3600) return c;
        if (ctx) {
          ctx.waitUntil(svfDef(env, true).catch(() => {
          }));
          return c;
        }
      }
    } catch (e) {
    }
  }
  const R = await svRowsOne(env, await svSheetId(env));
  if (!R.ok) return R;
  const H = (R.header || []).map((h) => String(h || "").replace(/\s+/g, " ").trim()), rows = R.rows || [];
  const colVals = /* @__PURE__ */ __name((i) => {
    const m = {};
    let n = 0;
    rows.forEach((r) => {
      const v = String(r[i] == null ? "" : r[i]).replace(/\s+/g, " ").trim();
      if (!v) return;
      n++;
      m[v] = (m[v] || 0) + 1;
    });
    return { m, n };
  }, "colVals");
  const used = /* @__PURE__ */ new Set([0, 1, 2, 3, 20, 21]);
  const cfg = await svfCfg(env), hideSet = new Set((cfg.hide || []).map((h) => String(h).replace(/\s+/g, " ").trim()));
  const all = svfAllQ(), OWN = /* @__PURE__ */ new Set(["J\u0119zyk ankiety"]);
  all.forEach((q) => {
    if (q.type === "grid") [q.rows].concat(q.rowsKid ? [q.rowsKid] : []).forEach((rs) => rs.forEach((r) => OWN.add(svfHdr(q, r))));
    else OWN.add(svfHdr(q));
  });
  for (let i = 0; i < H.length; i++) if (OWN.has(H[i])) used.add(i);
  const map = {}, mapP = {}, mapAge = {};
  const find = /* @__PURE__ */ __name((pat, not, start) => {
    const rx = new RegExp(pat, "i"), no = not ? new RegExp(not, "i") : null;
    for (let i = Math.max(2, start || 2); i < H.length; i++) {
      if (used.has(i) || /\[.+\]\s*$/.test(H[i])) continue;
      if (rx.test(H[i]) && !(no && no.test(H[i]))) {
        used.add(i);
        return i;
      }
    }
    return null;
  }, "find");
  for (const q of all) {
    if (q.colIdx != null) {
      map[q.k] = q.colIdx;
      continue;
    }
    if (q.col) {
      const same = !q.after && all.find((z) => z !== q && !z.after && map[z.k] != null && z.col === q.col && z.colNot === q.colNot);
      if (same) map[q.k] = map[same.k];
      else {
        const st = q.after ? map[q.after] != null ? map[q.after] + 1 : null : 2;
        if (st != null) {
          const i = find(q.col, q.colNot, st);
          if (i != null) map[q.k] = i;
        }
      }
    }
    if (q.colP) {
      const i = find(q.colP, null, 2);
      if (i != null) mapP[q.k] = i;
    }
    if (q.ageCol) {
      const i = find(q.ageCol, null, 2);
      if (i != null) mapAge[q.k] = i;
    }
  }
  const grids = [];
  for (let i = 2; i < H.length; i++) {
    if (OWN.has(H[i])) continue;
    const g = H[i].match(/^(.*?)\s*\[(.+)\]\s*$/);
    if (!g) continue;
    used.add(i);
    const cv = colVals(i), common = Object.keys(cv.m).filter((k) => cv.m[k] >= 3 && k.length <= 60);
    const prev = grids[grids.length - 1];
    if (prev && prev.q === g[1]) {
      prev.rows.push({ i, r: g[2] });
      common.forEach((k) => {
        if (prev.opts.indexOf(k) < 0) prev.opts.push(k);
      });
    } else grids.push({ q: g[1], rows: [{ i, r: g[2] }], opts: common.slice() });
  }
  grids.forEach((g) => {
    if (g.opts.length && g.opts.every((o) => SVF_FREQ.some((w) => o.toLowerCase().startsWith(w))) && !g.opts.some((o) => /^nigdy/i.test(o))) g.opts.push("nigdy");
    g.opts = svfOrder(g.opts);
    if (g.opts.every((o) => SVF_FREQ.some((w) => o.toLowerCase().startsWith(w))) && g.opts.length < 4) g.opts = svfOrder(Array.from(new Set(g.opts.concat(["rzadko", "czasami", "cz\u0119sto", "zawsze"]))));
  });
  const rest = [], restAll = [];
  for (let i = 4; i < H.length; i++) {
    if (used.has(i) || !H[i] || SVF_HIDE.test(H[i]) || Object.values(map).includes(i)) continue;
    restAll.push({ i, q: H[i], hidden: hideSet.has(H[i]) });
    if (hideSet.has(H[i])) continue;
    const cv = colVals(i), common = Object.keys(cv.m).filter((k) => cv.m[k] >= 3 && k.length <= 80).sort((a, b) => cv.m[b] - cv.m[a]), cover = common.reduce((s, k) => s + cv.m[k], 0);
    if (cv.n >= 5 && common.length >= 2 && common.length <= 10 && cover >= cv.n * 0.8) rest.push({ i, q: H[i], type: "chips", opts: svfOrder(common), other: cover < cv.n });
    else rest.push({ i, q: H[i], type: (cv.n ? Object.keys(cv.m).reduce((s, k) => s + k.length * cv.m[k], 0) / cv.n : 0) > 50 ? "textarea" : "text" });
  }
  const who = colVals(2), whoOpts = Object.keys(who.m).sort((a, b) => who.m[b] - who.m[a]);
  const d = {
    at: Math.floor(Date.now() / 1e3),
    header: H,
    map,
    mapP,
    mapAge,
    grids,
    rest,
    rest_all: restAll,
    who_parent: whoOpts.find((o) => /rodzic|opiekun/i.test(o)) || "Rodzic",
    who_self: whoOpts.find((o) => !/rodzic|opiekun/i.test(o)) || "Osoba doros\u0142a",
    ts_sample: String((rows[rows.length - 1] || [])[0] || "")
  };
  try {
    await appSettingSet(env, "svf_def3", JSON.stringify(d));
  } catch (e) {
  }
  return d;
}
__name(svfDef, "svfDef");
__name2(svfDef, "svfDef");
function svfTs(sample) {
  const p = new Intl.DateTimeFormat("sv-SE", { timeZone: "Europe/Warsaw", year: "numeric", month: "2-digit", day: "2-digit", hour: "2-digit", minute: "2-digit", second: "2-digit", hourCycle: "h23" }).formatToParts(/* @__PURE__ */ new Date());
  const g = /* @__PURE__ */ __name((t) => (p.find((x) => x.type === t) || {}).value, "g");
  if (/^\d{4}-/.test(sample)) return g("year") + "-" + g("month") + "-" + g("day") + " " + g("hour") + ":" + g("minute") + ":" + g("second");
  const day = /^0\d\./.test(sample) ? g("day") : String(Number(g("day")));
  return day + "." + g("month") + "." + g("year") + " " + (/ 0\d:/.test(sample) ? g("hour") : String(Number(g("hour")))) + ":" + g("minute") + ":" + g("second");
}
__name(svfTs, "svfTs");
__name2(svfTs, "svfTs");
function svfHdr(q, row) {
  if (q.type === "grid") return (q.hq || q.q.a || q.q) + " [" + row + "]";
  return q.hdr || q.hq || (typeof q.q === "string" ? q.q : q.q.a);
}
__name(svfHdr, "svfHdr");
__name2(svfHdr, "svfHdr");
async function svfPage(env, url, ctx) {
  const test = url.searchParams.get("test") === "1";
  const lang = SVF_LANGS[url.searchParams.get("lang")] ? url.searchParams.get("lang") : "pl";
  const live = await appSettingGet(env, "svf_live") !== "0";
  const shell = /* @__PURE__ */ __name((body) => new Response(`<!doctype html><html lang="pl"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><meta name="robots" content="noindex"><title>Kwestionariusz pomocny przy rozpoznaniu j\u0105kania i blok\xF3w mowy ESBS</title><style>${SVF_CSS}</style></head><body><div class="w">${body}<p class="foot">Szko\u0142a Nowej Mowy \xB7 Rynek 3/1A, Miko\u0142\xF3w \xB7 <a href="https://www.nowamowa.com">nowamowa.com</a></p></div></body></html>`, { headers: { "Content-Type": "text/html; charset=utf-8", "Cache-Control": "no-store" } }), "shell");
  if (!test && !live) return shell('<div class="hd"><b>Szko\u0142a Nowej Mowy</b><h1>Ankieta przed kursem</h1></div><div class="card"><p>Ankieta jest w przygotowaniu. Prosimy skorzysta\u0107 z linku, kt\xF3ry dosta\u0142e\u015B(-a\u015B) od trenera.</p></div>');
  const D = await svfDef(env, test && url.searchParams.get("fresh") === "1", ctx);
  if (!D.header) return shell('<div class="card"><p>Nie uda\u0142o si\u0119 wczyta\u0107 ankiety: ' + ofEsc(D.error || "") + "</p></div>");
  const cfgL = await svfCfg(env);
  const STR = svfStrings(D), TRS = { pl: {} };
  await Promise.all(Object.keys(SVF_LANGS).filter((l) => l !== "pl").map(async (l) => {
    const m = await svfTrAll(env, l, STR, l !== lang);
    if (m) TRS[l] = m;
  }));
  const TR = TRS[lang] || {}, X = /* @__PURE__ */ __name((s) => TR[s] || s, "X");
  const LKA = {};
  Object.keys(SVF_LANGS).forEach((l) => {
    LKA[l] = { kid: svfLinksFor(cfgL, true, l), adult: svfLinksFor(cfgL, false, l) };
  });
  const sw = Object.keys(SVF_LANGS).map((l) => '<button type="button" class="flag' + (l === lang ? " on" : "") + '" data-lang="' + l + '" title="' + { pl: "Polski", sk: "Sloven\u010Dina", cs: "\u010Ce\u0161tina", en: "English" }[l] + '">' + SVF_FLAG[l] + "<span>" + { pl: "PL", sk: "SK", cs: "CZ", en: "EN" }[l] + "</span></button>").join("");
  const data = JSON.stringify({ test, lang, tr: TR, trs: TRS, linksAll: LKA, defC: { sk: "S\u0142owacja", cs: "Czechy" }, defCountry: { sk: "S\u0142owacja", cs: "Czechy" }[lang] || null, ctop: SVF_COUNTRY_TOP, countries: SVF_COUNTRIES, months: SVF_MONTHS, links: LKA[lang], steps: SVF_STEPS, grids: D.grids, rest: D.rest, map: test && url.searchParams.get("map") === "1" ? D.map : null, mapP: test && url.searchParams.get("map") === "1" ? D.mapP : null, header: test ? D.header : null, email: String(url.searchParams.get("e") || "").slice(0, 120) }).replace(/</g, "\\u003c");
  return shell('<div class="hd"><div style="display:flex;justify-content:flex-end"><div class="flags">' + sw + '</div></div><h1 id="h1">' + ofEsc(X("Kwestionariusz pomocny przy rozpoznaniu j\u0105kania i blok\xF3w mowy ESBS")) + '</h1><div class="hs" id="hs">' + ofEsc(X("ok. 10 minut \xB7 wi\u0119kszo\u015B\u0107 odpowiedzi to klikni\u0119cia \xB7 zapisuje si\u0119 na bie\u017C\u0105co")) + "</div></div>" + (test && url.searchParams.get("map") === "1" ? '<div class="card testn">\u{1F9EA} <b>Wersja testowa</b> \u2014 pod pytaniami wida\u0107, do kt\xF3rej kolumny arkusza trafia odpowied\u017A.</div>' : "") + '<div class="prog"><div class="pl"><span id="stl"></span><span id="stn"></span></div><div class="bar"><div id="pb"></div></div></div><form id="f" novalidate autocomplete="on"><div id="st"></div><input type="text" name="website" id="hp" tabindex="-1" autocomplete="off" style="position:absolute;left:-5000px"><div class="nav"><button type="button" class="b2" id="bk">' + ofEsc(X("\u2190 Wstecz")) + '</button><button type="submit" id="nx">' + ofEsc(X("Dalej \u2192")) + `</button></div><div id="msg"></div></form><script>var D=${data};${SVF_JS}<\/script>`);
}
__name(svfPage, "svfPage");
__name2(svfPage, "svfPage");
var SVF_CSS = ":root{--o:#D9822B;--o2:#FFF1DC;--ink:#2b2b2b;--mut:#6b6259;--line:#ead8c4}*{box-sizing:border-box}body{margin:0;background:#f4f1ed;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Arial,sans-serif;color:var(--ink);-webkit-text-size-adjust:100%}.w{max-width:680px;margin:0 auto;padding:14px 14px 40px}.hd{background:#1E1E1E;color:#fff;padding:20px 22px;border-top:6px solid #F5A623;border-radius:14px}.hd b{color:#F5A623;font-size:12px;letter-spacing:2px;text-transform:uppercase}.hd h1{margin:10px 0 0;font-size:22px;line-height:1.3}.hs{color:#cfc6b8;font-size:13px;margin-top:6px}.card{background:#fff;border:1px solid var(--line);border-radius:14px;padding:16px 18px;margin-top:12px}.testn{background:#FFF6E0;border-color:#F0D9A8;font-size:14px}.prog{margin:16px 2px 4px}.pl{display:flex;justify-content:space-between;font-size:13px;color:var(--mut);font-weight:600}.bar{background:#E8E0D3;height:8px;border-radius:4px;margin-top:6px;overflow:hidden}#pb{background:var(--o);height:8px;width:0;transition:width .3s}h2{font-size:21px;margin:14px 2px 2px}.note{color:var(--mut);font-size:14px;margin:4px 2px 0}.q{font-weight:650;font-size:16px;line-height:1.4;margin:0 0 10px}.q .req{color:#B3261E}.hint{color:var(--mut);font-size:13px;margin:-6px 0 10px}.map{color:#9a8f80;font-size:11.5px;margin-top:8px;font-family:ui-monospace,Menlo,monospace}input[type=text],input[type=email],input[type=tel],input[type=number],textarea{width:100%;padding:12px 13px;border:1.5px solid #ddd3c4;border-radius:10px;font-size:16px;font-family:inherit;background:#fff}input:focus,textarea:focus{outline:none;border-color:var(--o);box-shadow:0 0 0 3px #f5a62333}textarea{min-height:90px;resize:vertical}.two{display:grid;grid-template-columns:1fr 1fr;gap:10px}.two .card{margin-top:0}.grid2{display:grid;grid-template-columns:1fr 1fr;gap:12px;margin-top:12px}@media(max-width:520px){.grid2{grid-template-columns:1fr}}.chips{display:flex;flex-wrap:wrap;gap:8px}.chip{border:1.5px solid var(--line);background:#fbf8f5;padding:10px 14px;border-radius:22px;cursor:pointer;font-size:15px;user-select:none;line-height:1.2}.chip.on{border-color:var(--o);background:var(--o2);font-weight:650}.chip.on::before{content:'\u2713 ';color:var(--o)}.cards{display:grid;grid-template-columns:repeat(auto-fit,minmax(180px,1fr));gap:10px}.cd{border:1.5px solid var(--line);background:#fbf8f5;border-radius:12px;padding:14px;cursor:pointer}.cd b{display:block;font-size:16px}.cd span{color:var(--mut);font-size:13px}.cd.on{border-color:var(--o);background:var(--o2);box-shadow:0 0 0 3px #f5a62322}.scale{display:flex;gap:6px}.scale .chip{flex:1;text-align:center;padding:11px 0;border-radius:10px}.scale .chip.on::before{content:''}.sl{display:flex;justify-content:space-between;color:var(--mut);font-size:12.5px;margin-top:5px}.gr{padding:10px 0;border-top:1px solid #f1e8dc}.gr:first-of-type{border-top:0;padding-top:0}.gr .r{font-size:15px;margin-bottom:7px}.gr .chips{gap:6px}.gr .chip{padding:8px 11px;font-size:14px}.gr .chip.on::before{content:''}.ck{display:flex;gap:12px;align-items:flex-start;cursor:pointer;font-size:15px;line-height:1.45}.ck input{width:22px;height:22px;margin:1px 0 0;accent-color:var(--o);flex:none}.oth{margin-top:8px}.nav{display:flex;gap:10px;margin-top:18px;position:sticky;bottom:0;padding:10px 0;background:linear-gradient(transparent,#f4f1ed 30%)}button{flex:2;background:var(--o);color:#fff;border:0;padding:15px 20px;border-radius:12px;font-size:17px;font-weight:700;cursor:pointer;font-family:inherit}button.b2{flex:1;background:#fff;color:var(--ink);border:1.5px solid var(--line)}button:disabled{opacity:.6}.dps{display:grid;grid-template-columns:1fr 1.6fr 1.2fr;gap:8px}.dp{display:flex;flex-direction:column;gap:4px;font-size:12.5px;color:var(--mut);font-weight:600}.dp select{font-size:17px;padding:12px 10px;border:1.5px solid #ddd3c4;border-radius:10px;background:#fff;font-family:inherit;color:var(--ink)}.dp select:focus{outline:none;border-color:var(--o)}.geo{background:#fff;color:var(--ink);border:1.5px solid var(--line);padding:9px 14px;border-radius:20px;font-size:14px;font-weight:600;width:auto}.lk{display:flex;gap:12px;align-items:center;text-decoration:none;color:var(--ink);border:1px solid var(--line);border-radius:12px;padding:10px;margin:10px 0;text-align:left;background:#fffdf9}.lk.vid{flex-direction:column;align-items:stretch}.th{position:relative;border-radius:10px;overflow:hidden;aspect-ratio:16/9;background:#000}.th img{width:100%;height:100%;object-fit:cover;display:block}.th span{position:absolute;inset:0;margin:auto;width:62px;height:44px;border-radius:12px;background:#D9822Bee;color:#fff;font-size:22px;display:flex;align-items:center;justify-content:center}.book img{width:84px;border-radius:4px;box-shadow:3px 3px 10px #0003}.cover{flex:none;width:84px;height:120px;border-radius:3px 6px 6px 3px;background:linear-gradient(135deg,#1E1E1E,#3a2d22);color:#fff;padding:10px 8px;display:flex;flex-direction:column;justify-content:space-between;box-shadow:3px 3px 10px #0003;border-left:5px solid #F5A623}.cover small{color:#F5A623;font-size:9px;letter-spacing:1px;text-transform:uppercase}.cover b{font-size:11px;line-height:1.2;overflow-wrap:anywhere;hyphens:auto}.btnx{color:var(--o);font-weight:700;margin-top:6px}.btnl{display:inline-block;background:#D9822B;color:#fff;padding:12px 18px;border-radius:10px;text-decoration:none;font-weight:700}.gr.rerr{background:#FDECEC;border-radius:8px;padding-left:8px;padding-right:8px}.gr.rerr .r{color:#B3261E;font-weight:600}.sendp{display:none;margin-top:10px}.sendl{display:flex;justify-content:space-between;font-size:14px;color:var(--mut);font-weight:600}.sendp .bar{height:10px}#spb{background:var(--o);height:10px;width:0;transition:width .15s}.flags{display:flex;gap:6px}.flag{flex:none;display:flex;flex-direction:column;align-items:center;gap:3px;background:transparent;border:2px solid transparent;border-radius:10px;padding:5px 6px 3px;cursor:pointer;color:#cfc6b8;font-size:11px;font-weight:700;width:auto}.flag svg{display:block;border-radius:4px;box-shadow:0 0 0 1px #ffffff40}.flag.on{border-color:#F5A623;color:#F5A623;background:#ffffff12}.flag:hover{background:#ffffff1a}.err{border-color:#B3261E!important;box-shadow:0 0 0 3px #b3261e22}#msg{color:#B3261E;font-size:14px;margin-top:6px;min-height:18px}.foot{text-align:center;color:var(--mut);font-size:12px;margin-top:22px}a{color:var(--o)}.done{text-align:center;padding:30px 18px}.done .big{font-size:44px}";
var SVF_JS = String.raw`
var KEY='nm_ankieta2_'+(D.test?'test':'live'),S={},cur=0,t0=Date.now();try{var sv=JSON.parse(localStorage.getItem(KEY)||'{}');S=sv.a||{};cur=sv.s||0;}catch(e){}
if(D.email&&!S.email)S.email=D.email;
D.steps.forEach(function(s){(s.q||[]).forEach(function(q){if(q.def&&S[q.k]==null)S[q.k]=(q.k==='country'&&D.defCountry)?D.defCountry:(q.k==='country'&&D.lang==='en'?'':q.def);});});
function esc(t){return String(t==null?'':t).replace(/[&<>"]/g,function(c){return{'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;'}[c];});}
function isP(){return S.role==='rodzic';}
function isKid(){return (S.role==='uczen'&&S.school!=='Studia')||(S.role==='rodzic'&&S.stage&&S.stage!=='dorosle');}
function O(q){return isKid()&&q.optsKid?q.optsKid:q.opts;}
function R(q){return isKid()&&q.rowsKid?q.rowsKid:q.rows;}
function age(d){if(!d)return null;var b=new Date(d+'T12:00:00'),n=new Date(),a=n.getFullYear()-b.getFullYear();if(n.getMonth()<b.getMonth()||(n.getMonth()===b.getMonth()&&n.getDate()<b.getDate()))a--;return a;}
function tr(s){return D.tr&&D.tr[s]!=null&&D.tr[s]!==''?D.tr[s]:s;}
function T(x){return tr(typeof x==='string'?x:(isP()?(x.p||x.a):x.a));}
function cond(c){if(!c)return true;if(c.or)return c.or.some(cond);if(c.kid&&!isKid())return false;if(c.role&&c.role.indexOf(S.role)<0)return false;if(c.stage&&c.stage.indexOf(S.stage)<0)return false;if(c.ans){for(var k in c.ans){var v=S[k],ok=Array.isArray(v)?v.some(function(x){return c.ans[k].indexOf(x)>=0;}):c.ans[k].indexOf(v)>=0;if(!ok)return false;}}return true;}
function steps(){return D.steps.filter(function(s){if(s.rest)return D.rest.length>0;if(s.grids)return D.grids.length>0;return s.q.some(function(q){return cond(q.show);});});}
function mapTxt(i){return D.map!==null&&i!=null?'<div class="map">→ kolumna '+(i+1)+': '+esc(String(D.header[i]||'').slice(0,90))+'</div>':(D.map!==null?'<div class="map">→ nowa kolumna</div>':'');}
function chips(k,opts,multi,other){var v=S[k],arr=multi?(Array.isArray(v)?v:[]):[v];var vals=opts.map(function(o){return Array.isArray(o)?o[0]:o;});var oth=multi?arr.filter(function(x){return vals.indexOf(x)<0&&x;}).join(', '):(v&&vals.indexOf(v)<0?v:'');
  return '<div class="chips">'+opts.map(function(o){var val=Array.isArray(o)?o[0]:o,lab=Array.isArray(o)?o[1]:o;return '<span class="chip'+(arr.indexOf(val)>=0?' on':'')+'" data-k="'+k+'" data-v="'+esc(val)+'" data-m="'+(multi?1:0)+'" role="button" tabindex="0">'+esc(tr(lab))+'</span>';}).join('')+(other?'<span class="chip'+(oth||S['_o_'+k]?' on':'')+'" data-k="'+k+'" data-v="__o" data-m="'+(multi?1:0)+'" role="button" tabindex="0">'+esc(tr('Inne…'))+'</span>':'')+'</div>'+(other?'<input type="text" class="oth" data-oth="'+k+'" placeholder="'+esc(tr('Wpisz…'))+'" value="'+esc(oth)+'" style="display:'+(oth||S['_o_'+k]?'block':'none')+'">':'');}
function dateSel(k){var p=S['_d_'+k]||{},v=S[k];if(v&&/^\d{4}-\d{2}-\d{2}$/.test(v)){p={y:v.slice(0,4),m:String(+v.slice(5,7)),d:String(+v.slice(8,10))};}
  var Y=new Date().getFullYear(),ys=[];for(var y=Y;y>=1930;y--)ys.push(y);
  var sel=function(part,lab,opts){return '<label class="dp"><span>'+esc(tr(lab))+'</span><select data-dp="'+k+'" data-part="'+part+'"><option value="">—</option>'+opts.map(function(o){return '<option value="'+o[0]+'"'+(String(p[part]||'')===String(o[0])?' selected':'')+'>'+esc(o[1])+'</option>';}).join('')+'</select></label>';};
  var ds=[];for(var d=1;d<=31;d++)ds.push([d,d]);
  return '<div class="dps">'+sel('d','Dzień',ds)+sel('m','Miesiąc',D.months.map(function(m,i){return [i+1,tr(m)];}))+sel('y','Rok',ys.map(function(y){return [y,y];}))+'</div>'+(age(v)!=null?'<div class="hint" style="margin:8px 0 0">'+esc(tr('%1 lat').replace('%1',age(v)))+'</div>':'');}
function countrySel(k){var v=S[k]||'',top=D.ctop,oth=v&&top.indexOf(v)<0,open=oth||S['_o_'+k];
  var names=Object.keys(D.countries).map(function(c){return D.countries[c];}).filter(function(n){return top.indexOf(n)<0;}).sort(function(a,b){return tr(a).localeCompare(tr(b));});
  return '<div class="chips">'+top.map(function(n){return '<span class="chip'+(v===n?' on':'')+'" data-k="'+k+'" data-v="'+esc(n)+'" data-m="0" role="button" tabindex="0">'+esc(tr(n))+'</span>';}).join('')+'<span class="chip'+(open?' on':'')+'" data-k="'+k+'" data-v="__o" data-m="0" role="button" tabindex="0">'+esc(tr('Inny kraj…'))+'</span></div>'+
    (open?'<input type="text" list="ctl" class="oth" data-ctry="'+k+'" placeholder="'+esc(tr('Wyszukaj kraj…'))+'" value="'+esc(oth?tr(v):'')+'"><datalist id="ctl">'+names.map(function(n){return '<option value="'+esc(tr(n))+'">';}).join('')+'</datalist>':'')+
    '<div style="margin-top:10px"><button type="button" class="geo" data-geo="1">'+esc(tr('📍 Użyj mojej lokalizacji'))+'</button> <span class="hint" id="geoMsg" style="margin:0"></span></div>';}
function qHtml(q){if(!cond(q.show))return '';var r=q.req?' <span class="req">*</span>':'',h=q.hint?'<div class="hint">'+esc(T(q.hint))+'</div>':'',b='',mi=D.map?(isP()&&D.mapP&&D.mapP[q.k]!=null?D.mapP[q.k]:D.map[q.k]):null;
  if(q.k==='birth'&&S.role==='uczen'&&age(S.birth)!=null&&age(S.birth)<15)h+='<div class="hint" style="color:#B26A00;font-weight:600">'+esc(tr('Masz mniej niż 15 lat — poproś rodzica, żeby wypełnił ankietę razem z Tobą.'))+'</div>';
  if(q.type==='cards')return '<div class="card" data-q="'+q.k+'"><div class="q">'+esc(T(q.q))+r+'</div><div class="cards">'+q.opts.map(function(o){return '<div class="cd'+(S[q.k]===o[0]?' on':'')+'" data-k="'+q.k+'" data-v="'+o[0]+'" role="button" tabindex="0"><b>'+esc(tr(o[1]))+'</b>'+(o[2]?'<span>'+esc(tr(o[2]))+'</span>':'')+'</div>';}).join('')+'</div>'+mapTxt(mi)+'</div>';
  if(q.type==='check')return '<div class="card" data-q="'+q.k+'"><label class="ck"><input type="checkbox" data-k="'+q.k+'"'+(S[q.k]==='Tak'?' checked':'')+'><span>'+esc(T(q.q))+r+'</span></label>'+mapTxt(mi)+'</div>';
  if(q.type==='chips')b=chips(q.k,O(q),false,q.other);
  else if(q.type==='multi')b='<div class="hint" style="margin-top:-6px">'+esc(tr('możesz zaznaczyć kilka'))+'</div>'+chips(q.k,O(q),true,q.other);
  else if(q.type==='date')b=dateSel(q.k);
  else if(q.type==='country')b=countrySel(q.k);
  else if(q.type==='scale'){var o=[];for(var n=q.min;n<=q.max;n++)o.push(String(n));b='<div class="scale">'+o.map(function(x){return '<span class="chip'+(S[q.k]===x?' on':'')+'" data-k="'+q.k+'" data-v="'+x+'" data-m="0" role="button" tabindex="0">'+x+'</span>';}).join('')+'</div><div class="sl"><span>'+esc(tr(q.lo))+'</span><span>'+esc(tr(q.hi))+'</span></div>';}
  else if(q.type==='grid')b=R(q).map(function(rw,j){var kk=q.k+'_'+j;return '<div class="gr" data-row="'+kk+'"><div class="r">'+esc(tr(rw))+'</div>'+chips(kk,q.opts,false,false)+'</div>';}).join('');
  else if(q.type==='textarea')b='<textarea data-k="'+q.k+'">'+esc(S[q.k]||'')+'</textarea>';
  else{var tp=q.type==='email'?'email':q.type==='tel'?'tel':'text',im=q.type==='year'||q.type==='zip'?' inputmode="numeric"':'',ph=q.type==='year'?' placeholder="np. 1995" maxlength="4"':q.type==='zip'?' placeholder="00-000" maxlength="6"':'';b='<input type="'+tp+'" data-k="'+q.k+'"'+im+ph+(q.ac?' autocomplete="'+q.ac+'"':'')+' value="'+esc(S[q.k]||'')+'">';}
  return '<div class="card" data-q="'+q.k+'"><div class="q">'+esc(T(q.q))+r+'</div>'+h+b+mapTxt(q.type==='grid'?null:mi)+'</div>';}
function render(){var L=steps();if(cur>=L.length)cur=L.length-1;if(cur<0)cur=0;var s=L[cur],h='<h2>'+esc(T(s.t))+'</h2>'+(s.note?'<div class="note">'+esc(T(s.note))+'</div>':'');
  if(s.grids)h+=D.grids.map(function(g,gi){return '<div class="card" data-q="g'+gi+'"><div class="q">'+esc(tr(g.q))+'</div>'+g.rows.map(function(rw){var k='c'+rw.i;return '<div class="gr" data-row="'+k+'"><div class="r">'+esc(tr(rw.r))+'</div>'+chips(k,isP()?g.opts.concat(['nie wiem']):g.opts,false,false)+'</div>';}).join('')+(D.map?'<div class="map">→ kolumny '+(g.rows[0].i+1)+'–'+(g.rows[g.rows.length-1].i+1)+' (jak dotąd)</div>':'')+'</div>';}).join('');
  else if(s.rest)h+='<div class="note">'+esc(tr('Te pytania są z dotychczasowej ankiety.'))+'</div>'+D.rest.map(function(q){var k='c'+q.i;var b=q.type==='chips'?chips(k,q.opts,false,q.other):q.type==='textarea'?'<textarea data-k="'+k+'">'+esc(S[k]||'')+'</textarea>':'<input type="text" data-k="'+k+'" value="'+esc(S[k]||'')+'">';return '<div class="card" data-q="'+k+'"><div class="q">'+esc(tr(q.q))+'</div>'+b+(D.map?'<div class="map">→ kolumna '+(q.i+1)+'</div>':'')+'</div>';}).join('');
  else{var parts=s.q.map(function(q){return {q:q,h:qHtml(q)};}).filter(function(x){return x.h;}),i=0;while(i<parts.length){if(parts[i].q.half&&parts[i+1]&&parts[i+1].q.half){h+='<div class="grid2">'+parts[i].h.replace('class="card"','class="card" style="margin-top:0"')+parts[i+1].h.replace('class="card"','class="card" style="margin-top:0"')+'</div>';i+=2;}else{h+=parts[i].h;i++;}}}
  document.getElementById('st').innerHTML=h;document.getElementById('stl').textContent=T(s.t).replace(/^\S+\s/,'');document.getElementById('stn').textContent=tr('Krok %1 z %2').replace('%1',cur+1).replace('%2',L.length)+' · '+Math.round((cur+1)*100/L.length)+'%';document.getElementById('pb').style.width=Math.round((cur+1)*100/L.length)+'%';
  document.getElementById('bk').style.visibility=cur?'visible':'hidden';document.getElementById('nx').textContent=cur===L.length-1?tr('Wyślij ankietę ✓'):tr('Dalej →');document.getElementById('msg').textContent='';}
function save(){try{localStorage.setItem(KEY,JSON.stringify({a:S,s:cur}))}catch(e){}}
var F=document.getElementById('f');
F.addEventListener('click',function(e){var g=e.target.closest('[data-geo]');if(g){geo();return;}var c=e.target.closest('.chip,.cd');if(!c)return;var k=c.dataset.k,v=c.dataset.v,m=c.dataset.m==='1';
  if(c.classList.contains('cd')){S[k]=v;if(k==='role'&&v!=='rodzic')delete S.stage;save();render();return;}
  if(v==='__o'){var on=!S['_o_'+k];S['_o_'+k]=on;if(!on){if(m)S[k]=(S[k]||[]).filter(function(x){return Array.prototype.some.call(c.parentNode.querySelectorAll('.chip'),function(n){return n.dataset.v===x;});});else if(S[k]&&!Array.prototype.some.call(c.parentNode.querySelectorAll('.chip'),function(n){return n.dataset.v===S[k];}))delete S[k];}else if(!m)S[k]='';}
  else if(m){var a=Array.isArray(S[k])?S[k].slice():[];var ix=a.indexOf(v);if(ix>=0)a.splice(ix,1);else a.push(v);S[k]=a;}
  else{S[k]=S[k]===v?'':v;if(S[k])S['_o_'+k]=false;}
  save();var y=window.scrollY;render();window.scrollTo(0,y);
  if(v==='__o'&&S['_o_'+k]){var o=F.querySelector('[data-oth="'+k+'"]');if(o)o.focus();}});
F.addEventListener('keydown',function(e){if((e.key==='Enter'||e.key===' ')&&e.target.matches('.chip,.cd')){e.preventDefault();e.target.click();}});
F.addEventListener('input',function(e){var t=e.target;if(t.dataset.oth){var k=t.dataset.oth,c=F.querySelector('.chip[data-k="'+k+'"]'),m=c&&c.dataset.m==='1';var vals=Array.prototype.map.call(t.parentNode.querySelectorAll('.chip[data-k="'+k+'"]'),function(n){return n.dataset.v;});
    if(m){var a=(S[k]||[]).filter(function(x){return vals.indexOf(x)>=0;});if(t.value.trim())a.push(t.value.trim());S[k]=a;}else S[k]=t.value;save();return;}
  if(t.dataset.k){var k2=t.dataset.k;if(t.type==='checkbox')S[k2]=t.checked?'Tak':'';else{var v=t.value;if(t.inputMode==='numeric'&&k2==='zip'){v=v.replace(/[^\d]/g,'').slice(0,5);if(v.length>2)v=v.slice(0,2)+'-'+v.slice(2);t.value=v;}if(t.inputMode==='numeric'&&/year/.test(k2)){v=v.replace(/[^\d]/g,'').slice(0,4);t.value=v;}S[k2]=v;}save();}});
F.addEventListener('change',function(e){var t=e.target;
  if(t.dataset&&t.dataset.dp){var k=t.dataset.dp,p=S['_d_'+k]||{};p[t.dataset.part]=t.value;S['_d_'+k]=p;S[k]=(p.y&&p.m&&p.d)?(p.y+'-'+('0'+p.m).slice(-2)+'-'+('0'+p.d).slice(-2)):'';save();var y=window.scrollY;render();window.scrollTo(0,y);return;}
  if(t.dataset&&t.dataset.ctry){var val=t.value.trim(),hit=Object.keys(D.countries).map(function(c){return D.countries[c];}).find(function(n){return tr(n).toLowerCase()===val.toLowerCase()||n.toLowerCase()===val.toLowerCase();});S[t.dataset.ctry]=hit||val;save();var y2=window.scrollY;render();window.scrollTo(0,y2);return;}
  if(e.target.type==='checkbox'){S[e.target.dataset.k]=e.target.checked?'Tak':'';save();}if(e.target.type==='date'){S[e.target.dataset.k]=e.target.value;save();var y=window.scrollY;render();window.scrollTo(0,y);}});
document.getElementById('bk').onclick=function(){cur--;save();render();window.scrollTo(0,0);};
function check(){var L=steps(),s=L[cur],bad=[],rows=[];
  if(s.grids)D.grids.forEach(function(g,gi){var miss=g.rows.filter(function(rw){return !S['c'+rw.i];});if(miss.length){bad.push('g'+gi);miss.forEach(function(rw){rows.push('c'+rw.i);});}});
  if(s.q)s.q.forEach(function(q){if(!cond(q.show))return;if(q.type==='grid'){if(q.req){var mr=R(q).map(function(x,j){return q.k+'_'+j;}).filter(function(kk){return !S[kk];});if(mr.length){bad.push(q.k);rows=rows.concat(mr);}}return;}var v=S[q.k];var empty=Array.isArray(v)?!v.length:!String(v||'').trim();
  if(q.req&&empty)bad.push(q.k);if(q.type==='email'&&!empty&&!/^[^\s@]+@[^\s@]+\.[a-z]{2,}$/i.test(v))bad.push(q.k);if(q.type==='year'&&!empty&&!(+v>=1930&&+v<=new Date().getFullYear()))bad.push(q.k);if(q.type==='date'&&!empty&&!(age(v)>=0&&age(v)<=96))bad.push(q.k);if(q.type==='zip'&&!empty&&S.country==='Polska'&&!/^\d{2}-\d{3}$/.test(v))bad.push(q.k);});
  F.querySelectorAll('.err,.rerr').forEach(function(x){x.classList.remove('err');x.classList.remove('rerr');});bad.forEach(function(k){var b=F.querySelector('[data-q="'+k+'"]');if(b)b.classList.add('err');});rows.forEach(function(k){var r=F.querySelector('[data-row="'+k+'"]');if(r)r.classList.add('rerr');});
  if(bad.length){var f=F.querySelector(rows.length?'[data-row="'+rows[0]+'"]':'[data-q="'+bad[0]+'"]')||F.querySelector('[data-q="'+bad[0]+'"]');if(f)f.scrollIntoView({behavior:'smooth',block:'center'});document.getElementById('msg').textContent=rows.length?tr('Zaznacz odpowiedź w każdym wierszu.'):tr('Uzupełnij zaznaczone pola.');return false;}return true;}
F.onsubmit=async function(ev){ev.preventDefault();if(!check())return;var L=steps();if(cur<L.length-1){cur++;save();render();window.scrollTo(0,0);return;}
  var b=document.getElementById('nx');b.disabled=true;document.getElementById('msg').textContent='';b.textContent=tr('⏳ Wysyłam…');
  var sp=document.getElementById('sp');if(!sp){sp=document.createElement('div');sp.id='sp';sp.className='sendp';sp.innerHTML='<div class="sendl"><span>'+esc(tr('⏳ Wysyłam…'))+'</span><b id="spn">0%</b></div><div class="bar"><div id="spb"></div></div>';document.querySelector('.nav').after(sp);}sp.style.display='block';
  var pct=0,tick=setInterval(function(){pct+=(92-pct)*0.12;document.getElementById('spb').style.width=pct+'%';document.getElementById('spn').textContent=Math.round(pct)+'%';},150);
  var done=function(okk){clearInterval(tick);if(okk){document.getElementById('spb').style.width='100%';document.getElementById('spn').textContent='100%';}else sp.style.display='none';};
  var A={};for(var k in S){if(k.indexOf('_o_')===0)continue;A[k]=S[k];}
  try{var r=await fetch('/ankieta/submit',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({test:D.test,lang:D.lang,answers:A,website:document.getElementById('hp').value,ms:Date.now()-t0})});var d=await r.json();if(!r.ok||!d.ok)throw new Error(d.error||r.status);done(true);await new Promise(function(res){setTimeout(res,350);});var spx=document.getElementById('sp');if(spx)spx.style.display='none';
    try{localStorage.removeItem(KEY)}catch(x){}document.querySelector('.prog').style.display='none';document.querySelector('.nav').style.display='none';
    var LK=(isKid()?D.links.kid:D.links.adult)||[];document.getElementById('st').innerHTML='<div class="card done"><div class="big">🙏</div><h2>'+esc(tr('Dziękujemy!'))+'</h2><p>'+esc(tr('Ankieta została zapisana. Trener przeczyta odpowiedzi i odezwie się, żeby porozmawiać.'))+'</p>'+(LK.length?'<p style="margin-top:18px"><b>'+esc(tr('Zanim porozmawiamy — warto obejrzeć:'))+'</b></p>'+LK.map(linkCard).join('')+'<p class="note">'+esc(tr('Wysłaliśmy też te linki na Twój e-mail.'))+'</p>':'')+(D.test&&D.map?'<p class="note">🧪 Test: wiersz '+(d.sheet_ok?'dopisany do zakładki „'+esc(d.tab)+'”':'NIE został dopisany ('+esc(d.sheet_error||'')+')')+'.</p>':'')+'</div>';window.scrollTo(0,0);
  }catch(e){done(false);b.disabled=false;b.textContent=tr('Wyślij ankietę ✓');document.getElementById('msg').textContent=tr('Nie udało się wysłać:')+' '+e.message+'. '+tr('Odpowiedzi są zapamiętane — spróbuj za chwilę.');}};
function geo(){var m=document.getElementById('geoMsg');if(!navigator.geolocation){m.textContent=tr('Nie udało się pobrać lokalizacji.');return;}m.textContent=tr('⏳ Szukam lokalizacji…');
  navigator.geolocation.getCurrentPosition(function(pos){fetch('https://nominatim.openstreetmap.org/reverse?format=jsonv2&zoom=14&accept-language=pl&lat='+pos.coords.latitude+'&lon='+pos.coords.longitude).then(function(r){return r.json();}).then(function(d){var a=d.address||{},cc=String(a.country_code||'').toLowerCase();
    S.country=D.countries[cc]||a.country||S.country;S.city=a.city||a.town||a.village||a.municipality||a.hamlet||S.city||'';if(cc==='pl'&&a.postcode)S.zip=a.postcode;save();var y=window.scrollY;render();window.scrollTo(0,y);}).catch(function(){m.textContent=tr('Nie udało się pobrać lokalizacji.');});},function(){m.textContent=tr('Nie udało się pobrać lokalizacji.');},{timeout:12000,maximumAge:600000});}
function linkCard(l){if(l.book)return '<a class="lk book" href="'+esc(l.u)+'" target="_blank" rel="noopener">'+(l.img?'<img src="'+esc(l.img)+'" alt="">':'<div class="cover"><small>Nowa Mowa</small><b>'+esc(tr(l.book))+'</b></div>')+'<div><div class="note" style="margin:0">'+esc(tr('📚 Polecamy książkę'))+'</div><b>'+esc(tr(l.book))+'</b><div class="btnx">'+esc(tr('Zobacz w sklepie'))+' →</div></div></a>';
  if(l.th)return '<a class="lk vid" href="'+esc(l.u)+'" target="_blank" rel="noopener"><div class="th"><img src="'+esc(l.th)+'" alt=""><span>▶</span></div><b>'+esc(tr(l.t))+'</b></a>';
  return '<p><a href="'+esc(l.u)+'" target="_blank" rel="noopener" class="btnl">'+esc(tr(l.t))+'</a></p>';}
function setLang(l){
  function apply(m){D.lang=l;D.tr=m||{};D.links=D.linksAll[l];if(D.defC[l]&&(!S.country||S.country==='Polska'||Object.values(D.defC).indexOf(S.country)>=0))S.country=D.defC[l];if(l==='pl'&&Object.values(D.defC).indexOf(S.country)>=0)S.country='Polska';
    document.querySelectorAll('.flag').forEach(function(b){b.classList.toggle('on',b.dataset.lang===l);});document.getElementById('h1').textContent=tr('Kwestionariusz pomocny przy rozpoznaniu jąkania i bloków mowy ESBS');document.title=tr('Kwestionariusz pomocny przy rozpoznaniu jąkania i bloków mowy ESBS');document.getElementById('hs').textContent=tr('ok. 10 minut · większość odpowiedzi to kliknięcia · zapisuje się na bieżąco');document.getElementById('bk').textContent=tr('← Wstecz');document.documentElement.lang=l;
    try{var u=new URL(location.href);u.searchParams.set('lang',l);history.replaceState(null,'',u.pathname+u.search);}catch(e){}save();render();}
  if(l==='pl'||D.trs[l]){apply(D.trs[l]);return;}
  document.querySelectorAll('.flag').forEach(function(b){if(b.dataset.lang===l)b.style.opacity='.5';});
  fetch('/ankieta/tr?lang='+l).then(function(r){return r.json();}).then(function(d){D.trs[l]=d.tr||{};document.querySelectorAll('.flag').forEach(function(b){b.style.opacity='';});apply(D.trs[l]);}).catch(function(){document.querySelectorAll('.flag').forEach(function(b){b.style.opacity='';});});}
document.querySelectorAll('.flag').forEach(function(b){b.onclick=function(){setLang(b.dataset.lang);};});
render();
`;
async function svfSubmit(request, env, ctx) {
  let b = {};
  try {
    b = await request.json();
  } catch (e) {
  }
  if (b.website) return json({ ok: true });
  const test = !!b.test, live = await appSettingGet(env, "svf_live") !== "0";
  if (!test && !live) return json({ error: "Ankieta nie jest jeszcze w\u0142\u0105czona" }, 403);
  const A = b.answers && typeof b.answers === "object" ? b.answers : {};
  const val = /* @__PURE__ */ __name((k) => {
    const v = A[k];
    return (Array.isArray(v) ? v.map((x) => String(x || "").trim()).filter(Boolean).join(", ") : String(v == null ? "" : v)).replace(/\r/g, "").slice(0, 5e3).trim();
  }, "val");
  const email = val("email").toLowerCase();
  if (!/^[^\s@,;<>]+@[^\s@,;<>]+\.[a-z]{2,}$/i.test(email)) return json({ error: "Podaj poprawny adres e-mail" }, 400);
  if (val("consent") !== "Tak") return json({ error: "Potrzebna jest zgoda na przetwarzanie danych" }, 400);
  const D = await svfDef(env, false, ctx);
  if (!D.header) return json({ error: D.error || "Brak definicji ankiety" }, 500);
  const lang = SVF_LANGS[b.lang] ? b.lang : "pl";
  const H = D.header, parent = val("role") === "rodzic";
  const row = new Array(H.length).fill(""), ext = [];
  row[0] = svfTs(D.ts_sample);
  row[1] = email;
  row[2] = parent ? D.who_parent : D.who_self;
  const cap = /* @__PURE__ */ __name((t) => String(t || "").trim().replace(/\s+/g, " ").replace(/(^|[\s-])(\p{L})/gu, (m, a, c) => a + c.toUpperCase()), "cap");
  if (parent) {
    row[20] = (cap(val("pfirst")) + " " + cap(val("plast"))).trim();
    row[21] = (cap(val("cfirst")) + " " + (cap(val("clast")) || svSurnameFor(cap(val("plast")), svFemName(val("cfirst"))))).trim();
  } else row[3] = (cap(val("first")) + " " + cap(val("last"))).trim();
  const stage = val("stage"), kid = val("role") === "uczen" && val("school") !== "Studia" || parent && stage && stage !== "dorosle";
  const S0 = {};
  Object.keys(A).forEach((k) => {
    S0[k] = A[k];
  });
  const cond = /* @__PURE__ */ __name((c) => {
    if (!c) return true;
    if (c.or) return c.or.some(cond);
    if (c.kid && !kid) return false;
    if (c.role && c.role.indexOf(val("role")) < 0) return false;
    if (c.stage && c.stage.indexOf(stage) < 0) return false;
    if (c.ans) {
      for (const k in c.ans) {
        const v = S0[k], ok2 = Array.isArray(v) ? v.some((x) => c.ans[k].indexOf(x) >= 0) : c.ans[k].indexOf(v) >= 0;
        if (!ok2) return false;
      }
    }
    return true;
  }, "cond");
  const ageOf = /* @__PURE__ */ __name((d) => {
    const m = String(d).match(/^(\d{4})-(\d{2})-(\d{2})$/);
    if (!m) return null;
    const n = /* @__PURE__ */ new Date();
    let a = n.getFullYear() - +m[1];
    if (n.getMonth() + 1 < +m[2] || n.getMonth() + 1 === +m[2] && n.getDate() < +m[3]) a--;
    return a;
  }, "ageOf");
  const plDate = /* @__PURE__ */ __name((d) => {
    const m = String(d).match(/^(\d{4})-(\d{2})-(\d{2})$/);
    return m ? m[3] + "." + m[2] + "." + m[1] : d;
  }, "plDate");
  const SKIP = /* @__PURE__ */ new Set(["email", "first", "last", "pfirst", "plast", "cfirst", "clast"]);
  const put = /* @__PURE__ */ __name((i, v) => {
    if (i == null || !v) return;
    row[i] = row[i] ? row[i] + "; " + v : v;
  }, "put");
  const idx = /* @__PURE__ */ __name((k) => parent && D.mapP && D.mapP[k] != null ? D.mapP[k] : D.map[k], "idx");
  for (const q of svfAllQ()) {
    if (SKIP.has(q.k)) continue;
    const shown = cond(q.show);
    if (q.type === "grid") {
      const use = kid && q.rowsKid ? q.rowsKid : q.rows;
      [q.rows].concat(q.rowsKid ? [q.rowsKid] : []).forEach((rs) => rs.forEach((r, j) => ext.push([svfHdr(q, r), shown && rs === use ? val(q.k + "_" + j) : ""])));
      continue;
    }
    let v = shown ? val(q.k) : "";
    if (q.k === "role" || q.k === "stage") v = v ? (q.opts.find((o) => o[0] === v) || [v, v])[1].replace(/^\S+\s/, "") : "";
    if (q.type === "date" && v) {
      const i = idx(q.k), a = ageOf(v);
      if (q.ageCol && D.mapAge && D.mapAge[q.k] != null && a != null) put(D.mapAge[q.k], String(a));
      if (i != null && /wiek/i.test(H[i] || "") && !/data|urodz/i.test(H[i] || "")) {
        put(i, a != null ? String(a) : "");
        ext.push([svfHdr(q), plDate(v)]);
        continue;
      }
      v = plDate(v);
    }
    if (idx(q.k) != null) put(idx(q.k), v);
    else ext.push([svfHdr(q), v]);
  }
  ext.push(["J\u0119zyk ankiety", lang]);
  Object.keys(A).forEach((k) => {
    const m = k.match(/^c(\d+)$/);
    if (m && +m[1] > 1 && +m[1] < H.length) row[+m[1]] = val(k);
  });
  const now = Math.floor(Date.now() / 1e3);
  await env.DB.prepare("CREATE TABLE IF NOT EXISTS survey_online (id INTEGER PRIMARY KEY AUTOINCREMENT, created_at INTEGER, test INTEGER, email TEXT, data TEXT, sheet_tab TEXT, sheet_ok INTEGER, sheet_error TEXT, fill_ms INTEGER)").run();
  const ins = await env.DB.prepare("INSERT INTO survey_online (created_at, test, email, data, fill_ms) VALUES (?, ?, ?, ?, ?)").bind(now, test ? 1 : 0, email, JSON.stringify({ answers: A, row, ext }), Number(b.ms) || 0).run();
  const id = ins.meta && ins.meta.last_row_id;
  let tab = SVF_TEST_TAB, ok = false, err = "";
  const sheetJob = /* @__PURE__ */ __name(async () => {
    try {
      const sid = await svSheetId(env);
      const meta = await svGoogle(env, encodeURIComponent(sid) + "?fields=sheets.properties(title)");
      if (!meta.ok) throw new Error(meta.error);
      const titles = (meta.data.sheets || []).map((x) => x.properties.title);
      if (!test) tab = titles[0];
      if (titles.indexOf(tab) < 0) {
        const a = await svGoogle(env, encodeURIComponent(sid) + ":batchUpdate", { method: "POST", body: { requests: [{ addSheet: { properties: { title: tab, gridProperties: { frozenRowCount: 1 } } } }] } });
        if (!a.ok) throw new Error(a.error);
      }
      const q = /* @__PURE__ */ __name((r) => encodeURIComponent("'" + tab.replace(/'/g, "''") + "'!" + r), "q");
      const hv = await svGoogle(env, encodeURIComponent(sid) + "/values/" + q("1:1"));
      if (!hv.ok) throw new Error(hv.error);
      let head = ((hv.data.values || [])[0] || []).map(String);
      if (test) {
        const extra = head.slice(H.length);
        head = H.slice().concat(extra);
      }
      if (!head.length) throw new Error("Brak nag\u0142\xF3wk\xF3w w zak\u0142adce");
      const head0 = ((hv.data.values || [])[0] || []).map(String).join("");
      const pos = ext.map(([h]) => {
        let j = head.indexOf(h);
        if (j < 0) {
          head.push(h);
          j = head.length - 1;
        }
        return j;
      });
      if (head.join("") !== head0) {
        const w = await svGoogle(env, encodeURIComponent(sid) + "/values/" + q("A1") + "?valueInputOption=RAW", { method: "PUT", body: { values: [head] } });
        if (!w.ok) throw new Error(w.error);
      }
      const out = new Array(head.length).fill("");
      row.forEach((v, i) => {
        if (i < out.length) out[i] = v;
      });
      pos.forEach((j, k) => {
        if (ext[k][1] || !out[j]) out[j] = ext[k][1];
      });
      const ap = await svGoogle(env, encodeURIComponent(sid) + "/values/" + q("A1") + ":append?valueInputOption=RAW&insertDataOption=INSERT_ROWS", { method: "POST", body: { values: [out] } });
      if (!ap.ok) throw new Error(ap.error);
      ok = true;
    } catch (e) {
      err = String(e && e.message || e).slice(0, 300);
    }
    try {
      await env.DB.prepare("UPDATE survey_online SET sheet_tab = ?, sheet_ok = ?, sheet_error = ? WHERE id = ?").bind(tab, ok ? 1 : 0, err || null, id).run();
    } catch (e) {
    }
  }, "sheetJob");
  const mailJob = /* @__PURE__ */ __name(async () => {
    try {
      await ensureGoogleTables(env);
      const account = await appSettingGet(env, "google_account") || "";
      const who = parent ? row[21] + " (rodzic: " + row[20] + ")" : row[3];
      await ofGmailSend(env, ofMime({
        from: account,
        fromName: "Panel Nowej Mowy",
        to: ofReminderTo(env),
        replyTo: email,
        subject: (test ? "\u{1F9EA} Test: " : "\u{1F4DD} ") + "nowa ankieta \u2014 " + (who || email),
        html: '<div style="font-family:Arial,Helvetica,sans-serif;font-size:15px"><p>Nowa ankieta online: <b>' + ofEsc(who || "") + "</b> \xB7 " + ofEsc(email) + (val("phone") ? " \xB7 \u{1F4DE} " + ofEsc(val("phone")) : "") + "</p><p>Arkusz: " + (ok ? "\u2705 dopisano do zak\u0142adki \u201E" + ofEsc(tab) + "\u201D" : "\u274C nie dopisano (" + ofEsc(err) + ") \u2014 odpowiedzi s\u0105 zapisane w bazie panelu") + "</p></div>"
      }));
    } catch (e) {
    }
    try {
      const LK = svfLinksFor(await svfCfg(env), kid, lang);
      if (LK.length) {
        const TRm = await svfTr(env, lang, SVF_UI), X = /* @__PURE__ */ __name((s) => TRm[s] || s, "X");
        const account = await appSettingGet(env, "google_account") || "";
        const hi = parent ? X("Dzie\u0144 dobry,") : X("Cze\u015B\u0107") + (val("first") ? " " + cap(val("first")) : "") + ",";
        const html = '<div style="background:#f4f1ed;padding:16px 8px;font-family:Arial,Helvetica,sans-serif"><table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="max-width:560px;margin:0 auto;background:#fff;border-radius:10px;overflow:hidden"><tr><td style="background:#1E1E1E;padding:16px 24px;border-top:6px solid #F5A623"><span style="color:#F5A623;font-size:12px;letter-spacing:2px;font-weight:bold">SZKO\u0141A NOWEJ MOWY</span><div style="color:#fff;font-size:20px;font-weight:bold;margin-top:2px">' + ofEsc(X("\u{1F64F} Dzi\u0119kujemy za ankiet\u0119")) + '</div></td></tr><tr><td style="padding:22px 24px;font-size:15px;line-height:1.6;color:#2b2b2b"><p style="margin:0 0 12px">' + ofEsc(hi) + '</p><p style="margin:0 0 12px">' + ofEsc(X("dzi\u0119kujemy za wype\u0142nienie ankiety. Trener przeczyta odpowiedzi i odezwie si\u0119, \u017Ceby porozmawia\u0107.")) + '</p><p style="margin:0 0 8px"><b>' + ofEsc(X("Zanim porozmawiamy \u2014 warto obejrze\u0107:")) + "</b></p>" + LK.map((l) => l.th ? '<p style="margin:12px 0"><a href="' + ofEsc(l.u) + '" style="text-decoration:none;color:#2b2b2b"><img src="' + ofEsc(l.th) + '" width="480" style="width:100%;max-width:480px;border-radius:10px;display:block" alt=""><b style="display:block;margin-top:6px">\u25B6 ' + ofEsc(X(l.t)) + "</b></a></p>" : l.book ? '<table role="presentation" cellpadding="0" cellspacing="0" style="margin:12px 0"><tr>' + (l.img ? '<td style="padding-right:12px"><a href="' + ofEsc(l.u) + '"><img src="' + ofEsc(l.img) + '" width="90" style="width:90px;border-radius:4px" alt=""></a></td>' : "") + '<td style="vertical-align:middle"><span style="color:#8C7A5C;font-size:13px">' + ofEsc(X("\u{1F4DA} Polecamy ksi\u0105\u017Ck\u0119")) + "</span><br><b>" + ofEsc(X(l.book)) + '</b><br><a href="' + ofEsc(l.u) + '" style="color:#E8820C;font-weight:bold">' + ofEsc(X("Zobacz w sklepie")) + " \u2192</a></td></tr></table>" : '<p style="margin:8px 0"><a href="' + ofEsc(l.u) + '" style="display:inline-block;background:#E8820C;color:#fff;padding:11px 18px;border-radius:8px;text-decoration:none;font-weight:bold">' + ofEsc(X(l.t)) + "</a></p>").join("") + '<p style="margin:16px 0 0;color:#8C7A5C;font-size:13px">Szko\u0142a Nowej Mowy \xB7 Rynek 3/1A, Miko\u0142\xF3w</p></td></tr></table></div>';
        await ofGmailSend(env, ofMime({ from: account, fromName: "Nowa Mowa", to: email, subject: (test ? "[TEST] " : "") + X("Dzi\u0119kujemy za ankiet\u0119 \u2014 Nowa Mowa"), html }));
      }
    } catch (e) {
    }
  }, "mailJob");
  if (test) {
    await sheetJob();
    if (ctx) ctx.waitUntil(mailJob().catch(() => {
    }));
    else await mailJob();
  } else if (ctx) {
    ctx.waitUntil(sheetJob().then(mailJob).catch(() => {
    }));
    ok = true;
    tab = "g\u0142\xF3wna zak\u0142adka (w tle)";
  } else {
    await sheetJob();
    await mailJob();
  }
  return json({ ok: true, id, sheet_ok: ok, sheet_error: err || null, tab });
}
__name(svfSubmit, "svfSubmit");
__name2(svfSubmit, "svfSubmit");
var PW_Q = [
  { k: "flu", type: "scale", min: 1, max: 10, q: "Jak w tym tygodniu wygl\u0105da\u0142a mowa {imie_d}?", lo: "z du\u017Cym trudem", hi: "swobodnie, p\u0142ynnie" },
  { k: "old", type: "chips", q: "Jak cz\u0119sto pojawia\u0142a si\u0119 stara mowa (bloki, powt\xF3rzenia, przeci\u0105ganie)?", opts: ["Wcale", "Rzadko", "Czasami", "Cz\u0119sto", "Bardzo cz\u0119sto"] },
  { k: "days", type: "scale", min: 0, max: 7, q: "Ile dni w tym tygodniu {imie} {cw} (nagrania, \u0107wiczenia z trenerem)?", lo: "ani jednego", hi: "codziennie" },
  { k: "mins", type: "chips", q: "Ile czasu dziennie, \u015Brednio?", opts: ["do 10 minut", "10\u201320 minut", "20\u201340 minut", "ponad 40 minut"] },
  { k: "rec", type: "multi", q: "Co {imie} {robil} w tym tygodniu?", opts: ["Nagrania w aplikacji", "Telefony do innych kursant\xF3w", "\u0106wiczenia z trenerem", "\u0106wiczenia w terenie", "Nic z tych rzeczy"] },
  { k: "self", type: "chips", q: "Czy {imie} {sam} od siebie wykonuje trening \u2014 nagrania w domu i w terenie?", opts: ["Regularnie", "Cz\u0119sto", "Rzadko", "Tylko gdy przypomn\u0119", "M\xF3wi, \u017Ce {jej} si\u0119 nie chce", "Robi awantur\u0119"] },
  { k: "own", type: "chips", q: "Czy {imie} {sam} {uz} techniki Nowej Mowy poza \u0107wiczeniami?", opts: ["Zawsze", "Cz\u0119sto", "Czasem", "Rzadko", "Nie", "Nie wiem"] },
  { k: "field", type: "chips", q: "Kiedy ostatnio by\u0142(a) Pani/Pan z {imie_n} na \u0107wiczeniach w terenie?", opts: ["Dzi\u015B", "Wczoraj", "W tym tygodniu", "W zesz\u0142ym tygodniu", "Dawno", "Nie pami\u0119tam"] },
  { k: "good", type: "multi", q: "Gdzie {imie} {mowil} spokojn\u0105 Now\u0105 Mow\u0105?", opts: ["W domu", "Z rodze\u0144stwem", "Z kolegami", "Na przerwie", "Na lekcji / przy odpowiedzi", "W przedszkolu", "W sklepie", "Przez telefon", "Z obcymi osobami", "Pytanie o drog\u0119", "Przy czytaniu na g\u0142os", "W restauracji / kawiarni"], other: true },
  { k: "hard", type: "multi", q: "Gdzie {imie} {mial} problem z utrzymaniem techniki Nowej Mowy?", opts: ["W domu", "Z rodze\u0144stwem", "Z kolegami", "Na przerwie", "Na lekcji / przy odpowiedzi", "W przedszkolu", "W sklepie", "Przez telefon", "Z obcymi osobami", "Pytanie o drog\u0119", "Przy czytaniu na g\u0142os", "W restauracji / kawiarni"], other: true },
  { k: "tells", type: "chips", q: "Czy {imie} opowiada Pani/Panu o swoich do\u015Bwiadczeniach z Now\u0105 Mow\u0105?", opts: ["Tak, ch\u0119tnie", "Ma\u0142o", "Tylko gdy poprosz\u0119", "Nie", "Nigdy"] },
  { k: "mood", type: "scale", min: 1, max: 5, q: "Samopoczucie i motywacja {imie_d} w tym tygodniu", lo: "\u{1F61F} s\u0142abo", hi: "\u{1F600} \u015Bwietnie", labels: ["\u{1F61F}", "\u{1F641}", "\u{1F610}", "\u{1F642}", "\u{1F600}"] },
  { k: "moment", type: "textarea", q: "Najwa\u017Cniejszy moment tygodnia \u2014 sukces albo trudno\u015B\u0107", hint: "np. pierwszy raz {sam} {zam} co\u015B w sklepie" },
  { k: "ask", type: "textarea", q: "Pytanie albo pro\u015Bba do trenera (opcjonalnie)" },
  { k: "call", type: "check", q: "Prosz\u0119 o telefon od trenera" }
];
async function pwSig(env, sid) {
  const k = await crypto.subtle.importKey("raw", new TextEncoder().encode(await svReplySalt(env)), { name: "HMAC", hash: "SHA-256" }, false, ["sign"]);
  const s = await crypto.subtle.sign("HMAC", k, new TextEncoder().encode("weekly|" + String(sid)));
  return [...new Uint8Array(s)].slice(0, 10).map((b) => b.toString(16).padStart(2, "0")).join("");
}
__name(pwSig, "pwSig");
__name2(pwSig, "pwSig");
async function pwLink(env, base, sid, name) {
  return base + "/tydzien?s=" + encodeURIComponent(sid) + "&k=" + await pwSig(env, sid) + (name ? "&n=" + encodeURIComponent(String(name).split(" ")[0]) : "");
}
__name(pwLink, "pwLink");
__name2(pwLink, "pwLink");
function pwWeek(d) {
  const x = d ? new Date(d) : /* @__PURE__ */ new Date();
  const w = /* @__PURE__ */ new Date(new Intl.DateTimeFormat("sv-SE", { timeZone: "Europe/Warsaw" }).format(x) + "T12:00:00Z");
  w.setUTCDate(w.getUTCDate() - (w.getUTCDay() + 6) % 7);
  return w.toISOString().slice(0, 10);
}
__name(pwWeek, "pwWeek");
__name2(pwWeek, "pwWeek");
async function pwTables(env) {
  await env.DB.prepare("CREATE TABLE IF NOT EXISTS parent_weekly (id INTEGER PRIMARY KEY AUTOINCREMENT, ns_student_id TEXT, week TEXT, data TEXT, created_at INTEGER, updated_at INTEGER, UNIQUE(ns_student_id, week))").run();
  await env.DB.prepare("CREATE TABLE IF NOT EXISTS parent_weekly_sub (ns_student_id TEXT PRIMARY KEY, email TEXT, name TEXT, on_flag INTEGER, last_week TEXT, last_sent INTEGER, updated_by TEXT, updated_at INTEGER)").run();
}
__name(pwTables, "pwTables");
__name2(pwTables, "pwTables");
function pwIns(n) {
  const l = n.toLowerCase();
  if (/a$/.test(l)) return n.slice(0, -1) + "\u0105";
  if (/ek$/.test(l)) return n.slice(0, -2) + "kiem";
  if (/(k|g)$/.test(l)) return n + "iem";
  return n + "em";
}
__name(pwIns, "pwIns");
__name2(pwIns, "pwIns");
function pwGen(n) {
  if (!n) return "dziecka";
  const l = n.toLowerCase();
  if (/a$/.test(l)) {
    if (/[csznd]ia$/.test(l)) return n.slice(0, -1);
    if (/ia$/.test(l)) return n.slice(0, -1) + "i";
    if (/(l|k|g)a$/.test(l)) return n.slice(0, -1) + "i";
    return n.slice(0, -1) + "y";
  }
  if (/ek$/.test(l)) return n.slice(0, -2) + "ka";
  return n + "a";
}
__name(pwGen, "pwGen");
__name2(pwGen, "pwGen");
async function pwPage(request, env, url) {
  const sid = String(url.searchParams.get("s") || ""), sig = String(url.searchParams.get("k") || "");
  const name = String(url.searchParams.get("n") || "").replace(/[^\p{L} -]/gu, "").slice(0, 30);
  const shell = /* @__PURE__ */ __name((body) => new Response(`<!doctype html><html lang="pl"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><meta name="robots" content="noindex"><title>Tydzie\u0144 treningu \u2014 Nowa Mowa</title><style>${SVF_CSS}</style></head><body><div class="w">${body}<p class="foot">Szko\u0142a Nowej Mowy \xB7 Rynek 3/1A, Miko\u0142\xF3w \xB7 <a href="https://www.nowamowa.com">nowamowa.com</a></p></div></body></html>`, { headers: { "Content-Type": "text/html; charset=utf-8", "Cache-Control": "no-store" } }), "shell");
  if (!sid || sig !== await pwSig(env, sid)) return shell('<div class="hd"><b>Szko\u0142a Nowej Mowy</b><h1>Tydzie\u0144 treningu</h1></div><div class="card"><p>Link jest nieaktualny. Popro\u015B trenera o nowy link.</p></div>');
  await pwTables(env);
  const week = pwWeek(), prev = await env.DB.prepare("SELECT data FROM parent_weekly WHERE ns_student_id = ? AND week = ?").bind(sid, week).first();
  const fem = name ? svFemName(name) : null, g3 = /* @__PURE__ */ __name((m, f, n) => name ? fem ? f : m : n, "g3");
  const fill = /* @__PURE__ */ __name((t) => String(t).replace(/\{imie\}/g, name || "dziecko").replace(/\{imie_d\}/g, name ? pwGen(name) : "dziecka").replace(/\{cw\}/g, g3("\u0107wiczy\u0142", "\u0107wiczy\u0142a", "\u0107wiczy\u0142o")).replace(/\{sam\}/g, g3("sam", "sama", "samo")).replace(/\{uz\}/g, g3("u\u017Cywa\u0142", "u\u017Cywa\u0142a", "u\u017Cywa\u0142o")).replace(/\{robil\}/g, g3("robi\u0142", "robi\u0142a", "robi\u0142o")).replace(/\{mowil\}/g, g3("m\xF3wi\u0142", "m\xF3wi\u0142a", "m\xF3wi\u0142o")).replace(/\{mial\}/g, g3("mia\u0142", "mia\u0142a", "mia\u0142o")).replace(/\{jej\}/g, g3("mu", "jej", "mu")).replace(/\{zam\}/g, g3("zam\xF3wi\u0142", "zam\xF3wi\u0142a", "zam\xF3wi\u0142o")).replace(/\{imie_n\}/g, name ? pwIns(name) : "dzieckiem"), "fill");
  const qs = PW_Q.map((q) => Object.assign({}, q, { q: fill(q.q), hint: q.hint ? fill(q.hint) : q.hint, opts: q.opts ? q.opts.map(fill) : q.opts }));
  const wEnd = /* @__PURE__ */ new Date(week + "T12:00:00Z");
  wEnd.setUTCDate(wEnd.getUTCDate() + 6);
  const lbl = ofLong ? ofLong(week).replace(/ \d{4}$/, "") + " \u2013 " + ofLong(wEnd.toISOString().slice(0, 10)) : week;
  const data = JSON.stringify({ qs, prev: prev ? JSON.parse(prev.data || "{}") : null, s: sid, k: sig, week }).replace(/</g, "\\u003c");
  return shell('<div class="hd"><b>Szko\u0142a Nowej Mowy</b><h1>Tydzie\u0144 treningu' + (name ? " \u2014 " + ofEsc(name) : "") + '</h1><div class="hs">' + ofEsc(lbl) + ' \xB7 2 minuty \xB7 same klikni\u0119cia</div></div><div class="card" style="font-size:14.5px">Dzi\u0119kujemy, \u017Ce obserwuje Pani/Pan trening. Te kr\xF3tkie odpowiedzi co tydzie\u0144 pokazuj\u0105 trenerowi, jak mowa ' + ofEsc(name ? pwGen(name) : "dziecka") + " zmienia si\u0119 poza zaj\u0119ciami.</div>" + (prev ? '<div class="card testn">\u270F\uFE0F Odpowied\u017A z tego tygodnia ju\u017C jest \u2014 mo\u017Cesz j\u0105 poprawi\u0107.</div>' : "") + `<form id="f" novalidate><div id="st"></div><div class="nav"><button type="submit" id="nx">Wy\u015Blij \u2713</button></div><div id="msg"></div></form><script>var D=${data};${PW_JS}<\/script>`);
}
__name(pwPage, "pwPage");
__name2(pwPage, "pwPage");
var PW_JS = String.raw`
var S=D.prev||{};function esc(t){return String(t==null?'':t).replace(/[&<>"]/g,function(c){return{'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;'}[c];});}
function chips(q){var multi=q.type==='multi',v=S[q.k],arr=multi?(Array.isArray(v)?v:[]):[v],oth=multi?arr.filter(function(x){return q.opts.indexOf(x)<0&&x;}).join(', '):'';
  return '<div class="chips">'+q.opts.map(function(o){return '<span class="chip'+(arr.indexOf(o)>=0?' on':'')+'" data-k="'+q.k+'" data-v="'+esc(o)+'" data-m="'+(multi?1:0)+'" role="button" tabindex="0">'+esc(o)+'</span>';}).join('')+'</div>'+(q.other?'<input type="text" class="oth" data-oth="'+q.k+'" placeholder="Inne… (opcjonalnie)" value="'+esc(oth)+'">':'');}
function render(){document.getElementById('st').innerHTML=D.qs.map(function(q){var b='';
  if(q.type==='scale'){var o=[];for(var n=q.min;n<=q.max;n++)o.push(String(n));b='<div class="scale">'+o.map(function(x,i){return '<span class="chip'+(String(S[q.k])===x?' on':'')+'" data-k="'+q.k+'" data-v="'+x+'" data-m="0" role="button" tabindex="0">'+(q.labels?q.labels[i]:x)+'</span>';}).join('')+'</div><div class="sl"><span>'+esc(q.lo)+'</span><span>'+esc(q.hi)+'</span></div>';}
  else if(q.type==='chips'||q.type==='multi')b=(q.type==='multi'?'<div class="hint" style="margin-top:-6px">możesz zaznaczyć kilka</div>':'')+chips(q);
  else if(q.type==='textarea')b=(q.hint?'<div class="hint">'+esc(q.hint)+'</div>':'')+'<textarea data-k="'+q.k+'">'+esc(S[q.k]||'')+'</textarea>';
  else if(q.type==='check')return '<div class="card"><label class="ck"><input type="checkbox" data-k="'+q.k+'"'+(S[q.k]==='Tak'?' checked':'')+'><span>'+esc(q.q)+'</span></label></div>';
  return '<div class="card" data-q="'+q.k+'"><div class="q">'+esc(q.q)+'</div>'+b+'</div>';}).join('');}
var F=document.getElementById('f');
F.addEventListener('click',function(e){var c=e.target.closest('.chip');if(!c)return;var k=c.dataset.k,v=c.dataset.v;if(c.dataset.m==='1'){var a=Array.isArray(S[k])?S[k].slice():[];var i=a.indexOf(v);if(i>=0)a.splice(i,1);else a.push(v);S[k]=a;}else S[k]=S[k]===v?'':v;var y=window.scrollY;render();window.scrollTo(0,y);});
F.addEventListener('keydown',function(e){if((e.key==='Enter'||e.key===' ')&&e.target.matches('.chip')){e.preventDefault();e.target.click();}});
F.addEventListener('input',function(e){var t=e.target;if(t.dataset.oth){var q=D.qs.find(function(z){return z.k===t.dataset.oth;});var a=(S[q.k]||[]).filter(function(x){return q.opts.indexOf(x)>=0;});if(t.value.trim())a.push(t.value.trim());S[q.k]=a;}else if(t.dataset.k){S[t.dataset.k]=t.type==='checkbox'?(t.checked?'Tak':''):t.value;}});
F.addEventListener('change',function(e){if(e.target.type==='checkbox')S[e.target.dataset.k]=e.target.checked?'Tak':'';});
F.onsubmit=async function(ev){ev.preventDefault();var miss=['flu','old','days','mood'].filter(function(k){return !S[k];});F.querySelectorAll('.err').forEach(function(x){x.classList.remove('err');});
  if(miss.length){miss.forEach(function(k){var b=F.querySelector('[data-q="'+k+'"]');if(b)b.classList.add('err');});F.querySelector('[data-q="'+miss[0]+'"]').scrollIntoView({behavior:'smooth',block:'center'});document.getElementById('msg').textContent='Zaznacz odpowiedzi w wyróżnionych pytaniach.';return;}
  var b=document.getElementById('nx');b.disabled=true;b.textContent='⏳ Wysyłam…';
  try{var r=await fetch('/tydzien',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({s:D.s,k:D.k,a:S})});var d=await r.json();if(!r.ok||!d.ok)throw new Error(d.error||r.status);
    document.querySelector('.nav').style.display='none';document.getElementById('st').innerHTML='<div class="card done"><div class="big">💛</div><h2>Dziękujemy!</h2><p>Trener zobaczy odpowiedzi przed kolejnym kontaktem. Do zobaczenia za tydzień!</p></div>';window.scrollTo(0,0);
  }catch(e){b.disabled=false;b.textContent='Wyślij ✓';document.getElementById('msg').textContent='Nie udało się wysłać: '+e.message;}};
render();
`;
async function pwSubmit(request, env) {
  let b = {};
  try {
    b = await request.json();
  } catch (e) {
  }
  const sid = String(b.s || "");
  if (!sid || String(b.k || "") !== await pwSig(env, sid)) return json({ error: "Link jest nieaktualny" }, 403);
  const A = b.a && typeof b.a === "object" ? b.a : {}, clean = {};
  for (const q of PW_Q) {
    const v = A[q.k];
    if (v == null || v === "") continue;
    clean[q.k] = Array.isArray(v) ? v.map((x) => String(x).slice(0, 120)).slice(0, 15) : String(v).slice(0, 3e3);
  }
  if (!clean.flu || !clean.mood) return json({ error: "Brak odpowiedzi" }, 400);
  await pwTables(env);
  const week = pwWeek(), now = Math.floor(Date.now() / 1e3);
  await env.DB.prepare("INSERT INTO parent_weekly (ns_student_id, week, data, created_at, updated_at) VALUES (?, ?, ?, ?, ?) ON CONFLICT(ns_student_id, week) DO UPDATE SET data = excluded.data, updated_at = excluded.updated_at").bind(sid, week, JSON.stringify(clean), now, now).run();
  if (clean.call === "Tak" || clean.ask || Number(clean.flu) <= 3 || Number(clean.mood) <= 2) {
    try {
      const sub = await env.DB.prepare("SELECT email, name FROM parent_weekly_sub WHERE ns_student_id = ?").bind(sid).first();
      await ensureGoogleTables(env);
      const account = await appSettingGet(env, "google_account") || "";
      const nm = sub && sub.name || "kursant";
      await ofGmailSend(env, ofMime({
        from: account,
        fromName: "Panel Nowej Mowy",
        to: ofReminderTo(env),
        replyTo: sub && sub.email || null,
        subject: "\u{1F468}\u200D\u{1F469}\u200D\u{1F467} Tydzie\u0144 oczami rodzica: " + nm + (clean.call === "Tak" ? " \u2014 pro\u015Bba o telefon" : clean.ask ? " \u2014 pytanie" : " \u2014 s\u0142abszy tydzie\u0144"),
        html: '<div style="font-family:Arial,Helvetica,sans-serif;font-size:15px;line-height:1.55"><p><b>' + ofEsc(nm) + "</b> \xB7 tydzie\u0144 od " + ofEsc(week) + "</p><p>Mowa: <b>" + ofEsc(clean.flu) + "/10</b> \xB7 stara mowa: " + ofEsc(clean.old || "\u2014") + " \xB7 \u0107wiczenia: " + ofEsc(clean.days || "0") + " dni \xB7 samopoczucie: " + ofEsc(clean.mood) + "/5</p>" + (clean.moment ? "<p>\u2B50 " + ofEsc(clean.moment) + "</p>" : "") + (clean.ask ? '<blockquote style="border-left:4px solid #F5A623;margin:0;padding:6px 12px;background:#FFF6E8">\u2753 ' + ofEsc(clean.ask) + "</blockquote>" : "") + (clean.call === "Tak" ? "<p><b>\u{1F4DE} Rodzic prosi o telefon.</b></p>" : "") + "</div>"
      }));
    } catch (e) {
    }
  }
  return json({ ok: true, week });
}
__name(pwSubmit, "pwSubmit");
__name2(pwSubmit, "pwSubmit");
function pwMailHtml(link, name) {
  return '<div style="background:#f4f1ed;padding:16px 8px;font-family:Arial,Helvetica,sans-serif"><table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="max-width:560px;margin:0 auto;background:#fff;border-radius:10px;overflow:hidden"><tr><td style="background:#1E1E1E;padding:16px 24px;border-top:6px solid #F5A623"><span style="color:#F5A623;font-size:12px;letter-spacing:2px;font-weight:bold">SZKO\u0141A NOWEJ MOWY</span><div style="color:#fff;font-size:20px;font-weight:bold;margin-top:2px">\u{1F468}\u200D\u{1F469}\u200D\u{1F467} Tydzie\u0144 treningu' + (name ? " \u2014 " + ofEsc(name) : "") + '</div></td></tr><tr><td style="padding:22px 24px;font-size:15px;line-height:1.6;color:#2b2b2b"><p style="margin:0 0 12px">Dzie\u0144 dobry,</p><p style="margin:0 0 12px">min\u0105\u0142 kolejny tydzie\u0144 treningu. Jak wygl\u0105da\u0142a mowa ' + ofEsc(name ? pwGen(name) : "dziecka") + '? Kilka klikni\u0119\u0107 (ok. 2 minuty) pomo\u017Ce trenerowi dopasowa\u0107 kolejne \u0107wiczenia.</p><p style="margin:16px 0"><a href="' + ofEsc(link) + '" style="display:inline-block;background:#E8820C;color:#fff;padding:12px 22px;border-radius:8px;text-decoration:none;font-weight:bold">\u{1F4DD} Wype\u0142nij \u2014 2 minuty</a></p><p style="margin:0;color:#8C7A5C;font-size:13px">Link jest osobisty \u2014 prosimy go nie przekazywa\u0107 dalej.</p></td></tr></table></div>';
}
__name(pwMailHtml, "pwMailHtml");
__name2(pwMailHtml, "pwMailHtml");
async function pwSend(env, sub, base) {
  await ensureGoogleTables(env);
  const account = await appSettingGet(env, "google_account") || "";
  const first = String(sub.name || "").split(" ")[0];
  const link = await pwLink(env, base, sub.ns_student_id, first);
  const s = await ofGmailSend(env, ofMime({ from: account, fromName: "Nowa Mowa", to: sub.email, subject: "Jak min\u0105\u0142 tydzie\u0144 treningu" + (first ? " \u2014 " + first : "") + "? \u{1F4DD}", html: pwMailHtml(link, first) }));
  if (s.ok) await env.DB.prepare("UPDATE parent_weekly_sub SET last_week = ?, last_sent = ? WHERE ns_student_id = ?").bind(pwWeek(), Math.floor(Date.now() / 1e3), sub.ns_student_id).run();
  return s;
}
__name(pwSend, "pwSend");
__name2(pwSend, "pwSend");
async function pwTick(env) {
  const p = new Intl.DateTimeFormat("en-GB", { timeZone: "Europe/Warsaw", weekday: "short", hour: "2-digit", hourCycle: "h23" }).formatToParts(/* @__PURE__ */ new Date());
  if ((p.find((x) => x.type === "weekday") || {}).value !== "Sun" || Number((p.find((x) => x.type === "hour") || {}).value) < 17) return;
  await pwTables(env);
  const wk = pwWeek(), base = (await svfCfg(env)).public_base || "https://newspeech-backend.mowaiemocje.workers.dev";
  const rows = (await env.DB.prepare("SELECT * FROM parent_weekly_sub WHERE on_flag = 1 AND (last_week IS NULL OR last_week < ?) LIMIT 30").bind(wk).all()).results || [];
  for (const r of rows) {
    const s = await pwSend(env, r, base);
    if (!s.ok) break;
  }
}
__name(pwTick, "pwTick");
__name2(pwTick, "pwTick");
async function pwPanel(request, env, url) {
  let body = {};
  if (request.method === "POST") {
    try {
      body = await request.json();
    } catch (e) {
    }
  }
  const src = request.method === "POST" ? body : Object.fromEntries(url.searchParams.entries());
  const who = await contactAuth(env, src.ns_token, src.ns_email, src.ns_server);
  if (!who) return json({ error: "Nieprawid\u0142owy lub wygas\u0142y token NS \u2014 zaloguj si\u0119 ponownie" }, 401);
  if (who.forbidden) return json({ error: "Tylko trener/admin" }, 403);
  const sid = String(src.student_id || "");
  if (!sid) return json({ error: "Brak student_id" }, 400);
  await pwTables(env);
  if (url.pathname === "/parent-weekly/sub" && request.method === "POST") {
    const email = String(body.email || "").trim().toLowerCase();
    if (body.on !== false && !/^[^\s@,;<>]+@[^\s@,;<>]+\.[a-z]{2,}$/i.test(email)) return json({ error: "Podaj e-mail rodzica" }, 400);
    await env.DB.prepare("INSERT INTO parent_weekly_sub (ns_student_id, email, name, on_flag, updated_by, updated_at) VALUES (?, ?, ?, ?, ?, ?) ON CONFLICT(ns_student_id) DO UPDATE SET email = excluded.email, name = excluded.name, on_flag = excluded.on_flag, updated_by = excluded.updated_by, updated_at = excluded.updated_at").bind(sid, email, String(body.name || "").slice(0, 120), body.on === false ? 0 : 1, String(who.email || ""), Math.floor(Date.now() / 1e3)).run();
    if (body.send_now) {
      const sub2 = await env.DB.prepare("SELECT * FROM parent_weekly_sub WHERE ns_student_id = ?").bind(sid).first();
      const s = await pwSend(env, sub2, (await svfCfg(env)).public_base || url.origin);
      if (!s.ok) return json({ error: s.error || "Nie uda\u0142o si\u0119 wys\u0142a\u0107" }, 502);
    }
  }
  const entries = ((await env.DB.prepare("SELECT week, data, updated_at FROM parent_weekly WHERE ns_student_id = ? ORDER BY week DESC LIMIT 52").bind(sid).all()).results || []).map((r) => ({ week: r.week, at: r.updated_at, a: JSON.parse(r.data || "{}") }));
  const sub = await env.DB.prepare("SELECT email, name, on_flag, last_sent FROM parent_weekly_sub WHERE ns_student_id = ?").bind(sid).first();
  return json({ ok: true, entries, sub: sub || null, link: await pwLink(env, (await svfCfg(env)).public_base || url.origin, sid, String(src.name || sub && sub.name || "").split(" ")[0]), questions: PW_Q.map((q) => ({ k: q.k, q: q.q, type: q.type })) });
}
__name(pwPanel, "pwPanel");
__name2(pwPanel, "pwPanel");
async function svRowsOne(env, id) {
  const meta = await svGoogle(env, encodeURIComponent(id) + "?fields=sheets.properties(title)");
  if (!meta.ok) return meta;
  const titles = (meta.data.sheets || []).map((x) => (x.properties || {}).title).filter((t) => t && t !== SV_PANEL_TAB && t !== SVF_TEST_TAB);
  if (!titles.length) titles.push("Arkusz1");
  const q = /* @__PURE__ */ __name((t) => "'" + t.replace(/'/g, "''") + "'!A1:ZZ20000", "q");
  const v = await svGoogle(env, encodeURIComponent(id) + "/values:batchGet?" + titles.map((t) => "ranges=" + encodeURIComponent(q(t))).join("&"));
  if (!v.ok) return v;
  let header = null;
  const rows = [], seen = /* @__PURE__ */ new Set(), tabs = [];
  (v.data.valueRanges || []).forEach((vr, k) => {
    const vals = vr.values || [], h = vals[0] || [];
    if (header && (h.length < 20 || String(h[1] || "").trim().toLowerCase() !== String(header[1] || "").trim().toLowerCase())) return;
    if (!header) header = h;
    let n = 0;
    for (const r of vals.slice(1)) {
      if (!(r && r.length > 2 && String(r[1] || r[0] || "").trim())) continue;
      const key = svKey(r);
      if (seen.has(key)) continue;
      seen.add(key);
      rows.push(r);
      n++;
    }
    tabs.push({ title: titles[k], rows: n });
  });
  return { ok: true, header: header || [], rows, tabs };
}
__name(svRowsOne, "svRowsOne");
__name2(svRowsOne, "svRowsOne");
async function sstTable(env) {
  await env.DB.prepare("CREATE TABLE IF NOT EXISTS survey_status (row_key TEXT PRIMARY KEY, offer_at INTEGER, contract_at INTEGER, variant INTEGER, severity INTEGER, note TEXT, updated_by TEXT, updated_at INTEGER)").run();
  try {
    await env.DB.prepare("ALTER TABLE survey_status ADD COLUMN no_contact INTEGER").run();
  } catch (e) {
  }
  await env.DB.prepare("CREATE TABLE IF NOT EXISTS survey_merge (row_key TEXT PRIMARY KEY, into_key TEXT, merged_by TEXT, merged_at INTEGER)").run();
  await env.DB.prepare("CREATE TABLE IF NOT EXISTS survey_hidden (row_key TEXT PRIMARY KEY, hidden_by TEXT, hidden_at INTEGER)").run();
  await env.DB.prepare("CREATE TABLE IF NOT EXISTS offer_meta (id INTEGER PRIMARY KEY AUTOINCREMENT, to_email TEXT, kind TEXT, variant INTEGER, variant_name TEXT, price INTEGER, sent_by TEXT, created_at INTEGER)").run();
  for (const c of ["html_key TEXT", "pdf_key TEXT", "subject TEXT", "pdf_name TEXT"]) {
    try {
      await env.DB.prepare("ALTER TABLE offer_meta ADD COLUMN " + c).run();
    } catch (e) {
    }
  }
}
__name(sstTable, "sstTable");
__name2(sstTable, "sstTable");
var SV_SEVERITY = ["", "lekkie", "\u015Brednie", "mocne"];
async function svSetStatus(env, who, b) {
  await sstTable(env);
  const key = String(b.row_key || "").slice(0, 300);
  if (!key) return { ok: false, error: "Brak ankiety" };
  const cur = await env.DB.prepare("SELECT * FROM survey_status WHERE row_key = ?").bind(key).first() || {};
  const now = Math.floor(Date.now() / 1e3), o = Object.assign({}, cur);
  if (b.offer !== void 0) o.offer_at = b.offer ? cur.offer_at || now : null;
  if (b.contract !== void 0) o.contract_at = b.contract ? cur.contract_at || now : null;
  if (b.variant !== void 0) o.variant = Math.max(0, Math.min(9, Number(b.variant) || 0)) || null;
  if (b.severity !== void 0) o.severity = Math.max(0, Math.min(3, Number(b.severity) || 0));
  if (b.note !== void 0) o.note = String(b.note || "").slice(0, 2e3);
  if (b.no_contact !== void 0) o.no_contact = b.no_contact ? 1 : 0;
  await env.DB.prepare("INSERT INTO survey_status (row_key, offer_at, contract_at, variant, severity, note, updated_by, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT(row_key) DO UPDATE SET offer_at = excluded.offer_at, contract_at = excluded.contract_at, variant = excluded.variant, severity = excluded.severity, note = excluded.note, updated_by = excluded.updated_by, updated_at = excluded.updated_at").bind(key, o.offer_at || null, o.contract_at || null, o.variant || null, o.severity || 0, o.note || "", String(who.email || ""), now).run();
  await env.DB.prepare("UPDATE survey_status SET no_contact = ? WHERE row_key = ?").bind(o.no_contact ? 1 : 0, key).run();
  return { ok: true, status: o };
}
__name(svSetStatus, "svSetStatus");
__name2(svSetStatus, "svSetStatus");
async function svBulkMail(env, who, b) {
  const okMail = /* @__PURE__ */ __name((x) => /^[^\s@,;<>]+@[^\s@,;<>]+\.[a-z]{2,}$/i.test(x), "okMail");
  let list = Array.isArray(b.items) ? b.items.map((it) => ({ to: String(it && it.to || "").trim().toLowerCase(), subject: String(it && it.subject || "").trim().slice(0, 200), html: String(it && it.html || "") })) : (Array.isArray(b.to) ? b.to : []).map((x) => ({ to: String(x || "").trim().toLowerCase(), subject: String(b.subject || "").trim().slice(0, 200), html: String(b.html || "") }));
  const seenTo = /* @__PURE__ */ new Set();
  list = list.filter((it) => okMail(it.to) && !seenTo.has(it.to) && seenTo.add(it.to)).slice(0, 150);
  let blocked = /* @__PURE__ */ new Set();
  try {
    await sstTable(env);
    blocked = new Set(((await env.DB.prepare("SELECT row_key FROM survey_status WHERE no_contact = 1").all()).results || []).map((x) => String(x.row_key).split("|").slice(1).join("|").toLowerCase()));
  } catch (e) {
  }
  const skipped = list.filter((it) => blocked.has(it.to)).map((it) => it.to);
  list = list.filter((it) => !blocked.has(it.to));
  if (!list.length) return { ok: false, error: skipped.length ? "Wszyscy adresaci maj\u0105 \u201Enie kontaktowa\u0107\u201D" : "Brak adresat\xF3w" };
  if (list.some((it) => !it.subject)) return { ok: false, error: "Wpisz temat" };
  if (list.some((it) => !it.html.replace(/<[^>]*>/g, "").trim())) return { ok: false, error: "Wpisz tre\u015B\u0107" };
  if (list.some((it) => it.html.length > 3e6)) return { ok: false, error: "Wiadomo\u015B\u0107 jest za du\u017Ca" };
  const logPrefix = b.kind === "why" ? "Pytanie o decyzj\u0119: " : b.kind === "why_reply" ? "Odpowied\u017A na pow\xF3d: " : b.kind === "reminder" ? "Przypomnienie: " : "Zbiorowa: ";
  const to = list.map((it) => it.to), subject = list[0].subject, html = list[0].html;
  await ensureGoogleTables(env);
  const account = await appSettingGet(env, "google_account") || "";
  await env.DB.prepare("CREATE TABLE IF NOT EXISTS email_log (id INTEGER PRIMARY KEY AUTOINCREMENT, ns_student_id TEXT, to_email TEXT, subject TEXT, sent_by TEXT, gmail_id TEXT, created_at INTEGER)").run();
  const sent = [], failed = [];
  const rowKeys = {};
  (Array.isArray(b.items) ? b.items : []).forEach((x) => {
    if (x && x.to && x.row_key) rowKeys[String(x.to).trim().toLowerCase()] = String(x.row_key).slice(0, 300);
  });
  const base = new URL(String(b._base || "https://newspeech-backend.mowaiemocje.workers.dev")).origin;
  for (const it of list) {
    const e = it.to;
    if (/__NM_KEEP__|__NM_STOP__|__NM_WHY_/.test(it.html)) {
      const L = await svReplyLinks(env, base, e, rowKeys[e] || "");
      it.html = it.html.split("__NM_KEEP__").join(L.keep).split("__NM_STOP__").join(L.stop).replace(/__NM_WHY_(\w+)__/g, (m, c) => L.why[c] || L.keep);
    }
    const s = await ofGmailSend(env, ofMime({ from: account, fromName: "Nowa Mowa", to: e, replyTo: who.email, subject: it.subject, html: it.html }));
    if (s.ok) {
      sent.push(e);
      try {
        await env.DB.prepare("INSERT INTO email_log (ns_student_id, to_email, subject, sent_by, gmail_id, created_at) VALUES (?, ?, ?, ?, ?, ?)").bind("", e, logPrefix + it.subject, who.email, s.id, Math.floor(Date.now() / 1e3)).run();
      } catch (x) {
      }
    } else {
      failed.push({ to: e, error: String(s.error || "").slice(0, 200) });
      if (/quota|rate|limit/i.test(String(s.error))) break;
    }
  }
  if (b.cc_me && sent.length) {
    try {
      await ofGmailSend(env, ofMime({ from: account, fromName: "Nowa Mowa", to: who.email, subject: "[kopia, wys\u0142ano do " + sent.length + "] " + subject, html }));
    } catch (x) {
    }
  }
  return { ok: true, sent: sent.length, failed, not_sent: to.length - sent.length - failed.length, skipped };
}
__name(svBulkMail, "svBulkMail");
__name2(svBulkMail, "svBulkMail");
async function svExportSheet(env) {
  const L = await svList(env);
  if (!L.ok) return L;
  const id = await svSheetId(env);
  const sids = [...new Set(L.items.map((x) => x.student && x.student.id).filter(Boolean).map(String))];
  const tr = {};
  if (sids.length) {
    const pg = new Client({ connectionString: env.HYPERDRIVE.connectionString });
    try {
      await pg.connect();
      for (const r of (await pg.query(`SELECT r.user_id::text AS sid, COUNT(*)::int AS n,
          SUM(CASE WHEN r.created_at > now() - interval '30 days' THEN 1 ELSE 0 END)::int AS n30,
          SUM(CASE WHEN r.reviewed_at IS NOT NULL THEN 1 ELSE 0 END)::int AS rev,
          SUM(CASE WHEN r.is_correct = true THEN 1 ELSE 0 END)::int AS ok,
          MIN(COALESCE(r.date, r.created_at::date))::text AS first, MAX(COALESCE(r.date, r.created_at::date))::text AS last
        FROM records r WHERE r.deleted_at IS NULL AND r.record_file IS NOT NULL AND r.user_id::text = ANY($1) GROUP BY 1`, [sids])).rows) tr[r.sid] = r;
    } catch (e) {
    } finally {
      try {
        await pg.end();
      } catch (e) {
      }
    }
  }
  const sym = {}, ed = {};
  try {
    await ssTable(env);
    for (const x of (await env.DB.prepare("SELECT * FROM student_symptoms").all()).results || []) sym[x.ns_student_id] = x;
  } catch (e) {
  }
  try {
    await seTable(env);
    for (const x of (await env.DB.prepare("SELECT * FROM survey_edits").all()).results || []) ed[x.ns_student_id] = x;
  } catch (e) {
  }
  const H = L.header || [], d = /* @__PURE__ */ __name((t) => t ? new Date(t * 1e3).toISOString().slice(0, 10) : "", "d");
  const editsTxt = /* @__PURE__ */ __name((e) => {
    if (!e) return "";
    let a = {}, ad = [];
    try {
      a = JSON.parse(e.answers || "{}");
    } catch (x) {
    }
    try {
      ad = JSON.parse(e.added || "[]");
    } catch (x) {
    }
    return Object.entries(a).map(([k, v]) => String(H[+k] || "kol. " + k).replace(/\s+/g, " ").slice(0, 60) + ": " + v).concat(ad.map((x) => (x.q || x.group || "") + ": " + (x.a || ""))).join(" | ");
  }, "editsTxt");
  const symTxt = /* @__PURE__ */ __name((x) => {
    if (!x) return "";
    let lv = {}, ex = [];
    try {
      lv = JSON.parse(x.levels || "{}");
    } catch (e) {
    }
    try {
      ex = JSON.parse(x.extra || "[]");
    } catch (e) {
    }
    const W = ["brak", "czasami", "cz\u0119sto", "zawsze"];
    return Object.entries(lv).map(([k, v]) => String(H[+k] || "kol. " + k).replace(/\s+/g, " ").replace(/^.*\[(.*)\]\s*$/, "$1").slice(0, 50) + ": " + W[v]).concat(ex.map((e) => e.name + ": " + W[e.level])).join(" | ");
  }, "symTxt");
  const head = [
    "Klucz ankiety (data|e-mail)",
    "Data ankiety",
    "Osoba",
    "E-mail",
    "Kursant w NS (id)",
    "Kursant w NS",
    "Oferta wys\u0142ana",
    "Umowa wys\u0142ana",
    "Wariant",
    "Start kursu",
    "Zadatek",
    "Przypomnienie 7 dni",
    "Nasilenie w rozmowie (trener)",
    "Notatka do statusu",
    "Nagrania razem",
    "Nagrania 30 dni",
    "Ocenione",
    "Poprawne %",
    "Pierwsze nagranie",
    "Ostatnie nagranie",
    "Objawy wg trenera",
    "Notatki trenera (objawy)",
    "Poprawki i informacje od trenera",
    "Aktualizacja",
    "Arkusz"
  ];
  const now = (/* @__PURE__ */ new Date()).toISOString().slice(0, 16).replace("T", " ");
  const rows = L.items.filter((x) => !x.hidden && !x.merged_into).map((x) => {
    const sid = x.student && x.student.id ? String(x.student.id) : "", t = tr[sid] || {}, b = x.booking || {};
    const e = sid && ed[sid] || ed["row:" + x.key], sy = sid ? sym[sid] : null;
    return [
      x.key,
      String(x.row[0] || ""),
      x.name || "",
      x.email || "",
      sid,
      x.student && x.student.name || "",
      d(x.offer_at),
      d(x.contract_at),
      x.variant ? "Wariant " + x.variant + (x.variant_name ? " \u2013 " + x.variant_name : "") : "",
      b.start_date || "",
      d(b.deposit_at),
      b.sent_at ? "wys\u0142ane " + d(b.sent_at) : b.deposit_at || b.remind_at ? "zaplanowane" : "",
      SV_SEVERITY[x.severity || 0] || "",
      x.status_note || "",
      t.n || (sid ? 0 : ""),
      t.n30 || (sid ? 0 : ""),
      t.rev || (sid ? 0 : ""),
      t.rev ? Math.round(100 * t.ok / t.rev) : "",
      t.first || "",
      t.last || "",
      symTxt(sy),
      sy && sy.notes || "",
      editsTxt(e),
      now,
      x.source || "PL"
    ];
  });
  const meta = await svGoogle(env, encodeURIComponent(id) + "?fields=sheets.properties(title,sheetId)");
  if (!meta.ok) return meta;
  if (!(meta.data.sheets || []).some((x) => x.properties.title === SV_PANEL_TAB)) {
    const a = await svGoogle(env, encodeURIComponent(id) + ":batchUpdate", { method: "POST", body: { requests: [{ addSheet: { properties: { title: SV_PANEL_TAB, gridProperties: { frozenRowCount: 1 } } } }] } });
    if (!a.ok) return a.code === "scope" || a.code === "no_access" ? Object.assign(a, { error: "Brak zgody na zapis w arkuszu: po\u0142\u0105cz konto Google ponownie (\u2699\uFE0F Admin \u2192 Kalendarz Google \u2192 Roz\u0142\u0105cz \u2192 Po\u0142\u0105cz) i daj kontu " + (await appSettingGet(env, "google_account") || "") + " uprawnienie \u201EEdytuj\u0105cy\u201D w arkuszu." }) : a;
  }
  const rng = encodeURIComponent("'" + SV_PANEL_TAB + "'!A1:ZZ50000");
  const c = await svGoogle(env, encodeURIComponent(id) + "/values/" + rng + ":clear", { method: "POST", body: {} });
  if (!c.ok) return c;
  const w = await svGoogle(env, encodeURIComponent(id) + "/values/" + encodeURIComponent("'" + SV_PANEL_TAB + "'!A1") + "?valueInputOption=RAW", { method: "PUT", body: { values: [head].concat(rows) } });
  if (!w.ok) return w;
  await appSettingSet(env, "survey_export_at", String(Math.floor(Date.now() / 1e3)));
  return { ok: true, rows: rows.length, tab: SV_PANEL_TAB, sheet_id: id };
}
__name(svExportSheet, "svExportSheet");
__name2(svExportSheet, "svExportSheet");
async function svList(env) {
  await ensureGoogleTables(env);
  await svLinksTable(env);
  const R = await svRows(env);
  if (!R.ok) return R;
  const links = {};
  for (const x of (await env.DB.prepare("SELECT row_key, ns_student_id FROM survey_links").all()).results || []) links[x.row_key] = x.ns_student_id;
  const emails = [...new Set(R.rows.map((r) => String(r[1] || "").trim().toLowerCase()).filter((e) => /@/.test(e)))];
  const names = R.rows.map((r) => svNameOf(r).name.toLowerCase()).filter(Boolean);
  const ids = Object.values(links).filter(Boolean);
  const users = {}, byEmail = {}, byName = {};
  const pg = new Client({ connectionString: env.HYPERDRIVE.connectionString });
  await pg.connect();
  try {
    const rows = (await pg.query(`SELECT u.id::text AS id, u.first_name, u.last_name, LOWER(u.email) AS email, u.created_at, u.role,
        (SELECT COUNT(*) FROM records r WHERE r.user_id = u.id AND r.deleted_at IS NULL)::int AS recs,
        (SELECT MAX(r.created_at) FROM records r WHERE r.user_id = u.id AND r.deleted_at IS NULL) AS last_rec,
        (SELECT MAX(d.date)::text FROM diaries d WHERE d.user_id = u.id AND d.deleted_at IS NULL) AS last_diary
      FROM users u WHERE LOWER(u.email) = ANY($1) OR u.id::text = ANY($2) OR LOWER(TRIM(COALESCE(u.first_name,'') || ' ' || COALESCE(u.last_name,''))) = ANY($3)`, [emails, ids, names])).rows;
    for (const u of rows) {
      const o = {
        id: u.id,
        name: ((u.first_name || "") + " " + (u.last_name || "")).trim(),
        email: u.email,
        created_at: u.created_at,
        recs: u.recs,
        role: u.role,
        last_rec: u.last_rec ? Math.floor(new Date(u.last_rec).getTime() / 1e3) : null,
        last_diary: u.last_diary ? String(u.last_diary).slice(0, 10) : null
      };
      users[u.id] = o;
      if (u.email) byEmail[u.email] = byEmail[u.email] || o;
      const nk = o.name.toLowerCase();
      (byName[nk] = byName[nk] || []).push(o);
    }
  } finally {
    try {
      await pg.end();
    } catch (e) {
    }
  }
  const sent = {}, book = {};
  try {
    for (const x of (await env.DB.prepare("SELECT LOWER(to_email) AS e, subject, created_at FROM email_log WHERE to_email IS NOT NULL").all()).results || []) {
      const s = sent[x.e] = sent[x.e] || {};
      if (!s.last || x.created_at > s.last) {
        s.last = x.created_at;
        s.last_subject = x.subject || "";
      }
      if (/^Przypomnienie:/i.test(x.subject || "")) s.rem = (s.rem || 0) + 1;
      if (/^Pytanie o decyzję:/i.test(x.subject || "")) s.why = Math.max(s.why || 0, x.created_at);
      if (/^Odpowiedź na powód:/i.test(x.subject || "")) s.whyRe = Math.max(s.whyRe || 0, x.created_at);
      if (/^Oferta/i.test(x.subject || "")) s.offer = Math.max(s.offer || 0, x.created_at);
      if (/^Rezerwacja/i.test(x.subject || "")) s.contract = Math.max(s.contract || 0, x.created_at);
    }
  } catch (e) {
  }
  const stat = {}, meta = {}, hidden = /* @__PURE__ */ new Set(), merged = {}, replies = {};
  const whys = {};
  try {
    for (const x of (await env.DB.prepare("SELECT email, action, message, phone, created_at FROM contact_replies ORDER BY created_at").all()).results || []) {
      if (/^why_/.test(x.action || "")) whys[x.email] = { code: x.action.slice(4), message: x.message || "", phone: x.phone || "", created_at: x.created_at };
      else replies[x.email] = x;
    }
  } catch (e) {
  }
  try {
    await sstTable(env);
    for (const x of (await env.DB.prepare("SELECT * FROM survey_status").all()).results || []) stat[x.row_key] = x;
    for (const x of (await env.DB.prepare("SELECT row_key FROM survey_hidden").all()).results || []) hidden.add(x.row_key);
    for (const x of (await env.DB.prepare("SELECT row_key, into_key FROM survey_merge").all()).results || []) merged[x.row_key] = x.into_key;
    for (const x of (await env.DB.prepare("SELECT id, LOWER(to_email) AS e, kind, variant, variant_name, price, created_at, html_key, pdf_key FROM offer_meta ORDER BY created_at").all()).results || []) {
      const m = meta[x.e] = meta[x.e] || {};
      m[x.kind] = x;
      if (x.html_key) {
        (m.docs = m.docs || []).push({ id: x.id, kind: x.kind, at: x.created_at, pdf: !!x.pdf_key, variant: x.variant });
      }
    }
  } catch (e) {
  }
  try {
    await ofTables(env);
    for (const x of (await env.DB.prepare("SELECT id, LOWER(to_email) AS e, start_date, deposit_at, sent_at, remind_at FROM course_reminders WHERE COALESCE(cancelled, 0) = 0").all()).results || []) {
      const b = book[x.e];
      if (!b || x.start_date > b.start_date) book[x.e] = x;
    }
  } catch (e) {
  }
  const edits = {}, addedBy = {};
  try {
    await seTable(env);
    for (const x of (await env.DB.prepare("SELECT ns_student_id, answers, added FROM survey_edits").all()).results || []) {
      try {
        edits[x.ns_student_id] = JSON.parse(x.answers || "{}");
      } catch (e) {
      }
      try {
        const a = JSON.parse(x.added || "[]");
        if (Array.isArray(a) && a.length) addedBy[x.ns_student_id] = a;
      } catch (e) {
      }
    }
  } catch (e) {
  }
  const langIdx = (R.header || []).findIndex((h) => String(h || "").trim() === "J\u0119zyk ankiety");
  const items = R.rows.map((r, i) => {
    const key = svKey(r), em = String(r[1] || "").trim().toLowerCase();
    const sidGuess = links[key] || byEmail[em] && byEmail[em].id || null;
    const ov = Object.assign({}, edits["row:" + key] || {}, sidGuess && edits[String(sidGuess)] || {});
    let rE = r;
    if (Object.keys(ov).length) {
      rE = r.slice();
      for (const [k, v] of Object.entries(ov)) rE[+k] = v;
    }
    const nm = svNameOf(rE);
    let stu = null, how = null, suggest = null;
    if (links[key] !== void 0) {
      stu = links[key] ? users[links[key]] || { id: links[key] } : null;
      how = links[key] ? "r\u0119cznie" : "od\u0142\u0105czone";
    } else if (byEmail[em]) {
      stu = byEmail[em];
      how = "e-mail";
    } else {
      const c = byName[nm.name.toLowerCase()];
      if (c && c.length === 1) suggest = c[0];
    }
    const st = stat[key] || {}, mt = meta[em] || {}, ctr = mt.contract || null, off = mt.offer || null;
    const tAns = Object.keys(ov).length ? ov : null, tAdd = [].concat(addedBy["row:" + key] || [], sidGuess && addedBy[String(sidGuess)] || []).map((x) => ({ group: String(x && x.group || ""), q: String(x && x.q || "").slice(0, 200), a: String(x && x.a || "").slice(0, 3e3) })).filter((x) => x.q || x.a);
    return {
      n: i + 2,
      key,
      row: r,
      trainer_answers: tAns,
      trainer_added: tAdd.length ? tAdd : null,
      child: nm.child,
      name: nm.name,
      parent: nm.parent,
      email: em,
      student: stu,
      matched_by: how,
      suggest,
      offer_at: Math.max((sent[em] || {}).offer || 0, st.offer_at || 0, off && off.created_at || 0) || null,
      contract_at: Math.max((sent[em] || {}).contract || 0, st.contract_at || 0, ctr && ctr.created_at || 0) || null,
      offer_manual: !!st.offer_at,
      contract_manual: !!st.contract_at,
      variant: st.variant || ctr && ctr.variant || null,
      variant_name: !st.variant && ctr && ctr.variant_name || null,
      offer_variant: off && off.variant || null,
      severity: st.severity || 0,
      status_note: st.note || "",
      no_contact: !!st.no_contact,
      last_contact_at: (sent[em] || {}).last || null,
      last_contact_subject: (sent[em] || {}).last_subject || "",
      reminders_sent: (sent[em] || {}).rem || 0,
      hidden: hidden.has(key),
      merged_into: merged[key] || null,
      reply: replies[em] || null,
      why: whys[em] || null,
      why_asked_at: (sent[em] || {}).why || null,
      why_reply_at: (sent[em] || {}).whyRe || null,
      sent_docs: (mt.docs || []).slice(-6),
      source: r._src || null,
      lang: r._lang || (langIdx >= 0 && /^(sk|cs|en)$/.test(String(r[langIdx] || "")) ? String(r[langIdx]) : null),
      test: !!r._test,
      booking: book[em] || null
    };
  });
  const byKey = {};
  items.forEach((x) => byKey[x.key] = x);
  items.forEach((x) => {
    if (x.merged_into && byKey[x.merged_into]) (byKey[x.merged_into].merged = byKey[x.merged_into].merged || []).push({ key: x.key, row: x.row, name: x.name, parent: x.parent, email: x.email, child: x.child });
  });
  return { ok: true, header: R.header, tabs: R.tabs || [], extra_errors: R.extra_errors || [], items };
}
__name(svList, "svList");
__name2(svList, "svList");
async function svForStudent(env, sid) {
  await svLinksTable(env);
  const R = await svRows(env);
  if (!R.ok) return R;
  const linked = (await env.DB.prepare("SELECT row_key FROM survey_links WHERE ns_student_id = ?").bind(String(sid)).all()).results || [];
  let row = null;
  if (linked.length) {
    const keys = linked.map((x) => x.row_key);
    row = R.rows.filter((r) => keys.includes(svKey(r))).pop() || null;
  }
  if (!row) {
    const pg = new Client({ connectionString: env.HYPERDRIVE.connectionString });
    await pg.connect();
    let em = null;
    try {
      em = ((await pg.query("SELECT LOWER(email) AS e FROM users WHERE id::text = $1", [String(sid)])).rows[0] || {}).e;
    } finally {
      try {
        await pg.end();
      } catch (e) {
      }
    }
    const off = new Set(((await env.DB.prepare("SELECT row_key FROM survey_links WHERE COALESCE(ns_student_id, '') = ''").all()).results || []).map((x) => x.row_key));
    let hid = /* @__PURE__ */ new Set();
    try {
      await sstTable(env);
      hid = new Set(((await env.DB.prepare("SELECT row_key FROM survey_hidden").all()).results || []).map((x) => x.row_key));
    } catch (e) {
    }
    if (em) row = R.rows.filter((r) => String(r[1] || "").trim().toLowerCase() === em && !off.has(svKey(r)) && !hid.has(svKey(r))).pop() || null;
  }
  let merged_rows = [];
  try {
    await sstTable(env);
    const mg = (await env.DB.prepare("SELECT row_key, into_key FROM survey_merge").all()).results || [];
    if (row) {
      const into = (mg.find((m) => m.row_key === svKey(row)) || {}).into_key;
      if (into) {
        const r2 = R.rows.find((r) => svKey(r) === into);
        if (r2) row = r2;
      }
    }
    if (row) {
      const k = svKey(row);
      const ks = mg.filter((m) => m.into_key === k).map((m) => m.row_key);
      merged_rows = R.rows.filter((r) => ks.includes(svKey(r)));
    }
  } catch (e) {
  }
  return { ok: true, header: R.header, row, row_key: row ? svKey(row) : null, merged_rows };
}
__name(svForStudent, "svForStudent");
__name2(svForStudent, "svForStudent");
async function trainerActivity(env) {
  const out = {}, now = Math.floor(Date.now() / 1e3), d90 = now - 90 * 86400;
  const put = /* @__PURE__ */ __name((em, k, v) => {
    em = String(em || "").toLowerCase();
    if (!em) return;
    const o = out[em] || (out[em] = {});
    o[k] = v;
  }, "put");
  const q = /* @__PURE__ */ __name(async (sql, ...b) => {
    try {
      return (await env.DB.prepare(sql).bind(...b).all()).results || [];
    } catch (e) {
      return [];
    }
  }, "q");
  for (const r of await q("SELECT LOWER(trainer_email) AS e, MAX(created_at) AS last, SUM(CASE WHEN created_at > ? THEN 1 ELSE 0 END) AS n90, COUNT(*) AS n FROM review_log GROUP BY 1", d90)) {
    put(r.e, "last_review", r.last);
    put(r.e, "reviews_90d", r.n90);
    put(r.e, "reviews", r.n);
  }
  for (const r of await q("SELECT LOWER(trainer_email) AS e, MAX(created_at) AS last FROM call_days GROUP BY 1")) put(r.e, "last_call_day", r.last);
  for (const r of await q("SELECT LOWER(sent_by) AS e, MAX(created_at) AS last FROM email_log GROUP BY 1")) put(r.e, "last_email", r.last);
  const pg = new Client({ connectionString: env.HYPERDRIVE.connectionString });
  try {
    await pg.connect();
    const cols = (await pg.query("SELECT column_name FROM information_schema.columns WHERE table_name = 'users' AND column_name IN ('last_sign_in_at','current_sign_in_at','updated_at')")).rows.map((r) => r.column_name);
    const sign = cols.filter((c) => /sign_in/.test(c));
    const signSql = sign.length ? "GREATEST(" + sign.map((c) => "u." + c).join(",") + ")" : "NULL";
    const rows = (await pg.query(`SELECT LOWER(u.email) AS e, ${signSql} AS last_sign_in,
        (SELECT COUNT(DISTINCT s.id) FROM students_groups_users sgu JOIN users s ON s.students_group_id = sgu.students_group_id AND s.deleted_at IS NULL WHERE sgu.user_id = u.id) AS students
      FROM users u WHERE u.role IN ('trainer','admin') AND u.deleted_at IS NULL`)).rows;
    for (const r of rows) {
      put(r.e, "last_sign_in", r.last_sign_in ? Math.floor(new Date(r.last_sign_in).getTime() / 1e3) : null);
      put(r.e, "students", +r.students || 0);
    }
  } catch (e) {
    out._pg_error = String(e && e.message || e).slice(0, 200);
  } finally {
    try {
      await pg.end();
    } catch (e) {
    }
  }
  for (const [em, o] of Object.entries(out)) {
    if (em[0] === "_") continue;
    o.last_any = Math.max(o.last_review || 0, o.last_call_day || 0, o.last_email || 0, o.last_sign_in || 0) || null;
  }
  const pgErr = out._pg_error || null;
  delete out._pg_error;
  return { ok: true, items: out, pg_error: pgErr };
}
__name(trainerActivity, "trainerActivity");
async function seTable(env) {
  await env.DB.prepare("CREATE TABLE IF NOT EXISTS survey_edits (ns_student_id TEXT PRIMARY KEY, answers TEXT, added TEXT, updated_by TEXT, updated_at INTEGER)").run();
}
__name(seTable, "seTable");
__name2(seTable, "seTable");
async function seGet(env, sid) {
  await seTable(env);
  const r = await env.DB.prepare("SELECT answers, added, updated_by, updated_at FROM survey_edits WHERE ns_student_id = ?").bind(String(sid)).first();
  let answers = {}, added = [];
  try {
    answers = r && r.answers ? JSON.parse(r.answers) : {};
  } catch (e) {
  }
  try {
    added = r && r.added ? JSON.parse(r.added) : [];
  } catch (e) {
  }
  return { answers, added, updated_by: r && r.updated_by, updated_at: r && r.updated_at };
}
__name(seGet, "seGet");
__name2(seGet, "seGet");
async function seMerged(env, sid, rowKey) {
  const st = sid ? await seGet(env, sid) : { answers: {}, added: [] };
  if (!rowKey) return st;
  const rk = await seGet(env, "row:" + rowKey);
  if (!rk.updated_at) return st;
  return { answers: Object.assign({}, rk.answers, st.answers), added: (rk.added || []).concat(st.added || []), updated_by: st.updated_by || rk.updated_by, updated_at: Math.max(st.updated_at || 0, rk.updated_at || 0) };
}
__name(seMerged, "seMerged");
__name2(seMerged, "seMerged");
async function ssTable(env) {
  await env.DB.prepare("CREATE TABLE IF NOT EXISTS student_symptoms (ns_student_id TEXT PRIMARY KEY, levels TEXT, extra TEXT, notes TEXT, updated_by TEXT, updated_at INTEGER)").run();
}
__name(ssTable, "ssTable");
__name2(ssTable, "ssTable");
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
    error: r.error || null,
    model: r.model || null
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
var TX_ROLE_RX = /^(sprzedaw|kasjer|pracownik|przechod|urzędni|urzedni|recepcj|kelner|trener|kursant|operator|lekarz|farmaceut|aptek|klient|konsultant|kierowc|pani|pan|mama|tata|matk|ojciec|brat|siostr|babci|dziad|koleg|koleżank|przyjaci|rodzin|nauczyciel|szef|znajom|sąsiad|ochroniarz|barman|fryzjer|doradc|rozmówc|osoba|mąż|żona|syn|córk|wujek|ciocia|ekspedient|obsług|portier|policjant|przewodnik|uczeń|uczennic|student)/iu;
function txCleanRole(role, text) {
  const r = String(role || "").trim();
  if (!r) return r;
  const low = " " + String(text || "").toLowerCase().replace(/[^\p{L}]+/gu, " ") + " ";
  const toks = r.split(/(\s+|,\s*)/);
  const out = [];
  let first = true;
  for (const t of toks) {
    if (!/\p{L}/u.test(t)) {
      out.push(t);
      continue;
    }
    const isCap = /^\p{Lu}/u.test(t);
    if (isCap && !first) {
      const stem = t.toLowerCase().slice(0, Math.max(3, t.length - 2));
      if (low.indexOf(" " + stem) < 0) {
        out.push("");
        continue;
      }
    }
    out.push(first && isCap && toks.filter((x) => /\p{L}/u.test(x)).length > 1 ? t.toLowerCase() : t);
    first = false;
  }
  const res = out.join("").replace(/\s*,\s*(,\s*)*/g, ", ").replace(/^[\s,]+|[\s,]+$/g, "").replace(/\s{2,}/g, " ");
  if (!res) return "rozm\xF3wca";
  if (/^\p{Lu}\p{Ll}+$/u.test(res) && TX_ROLE_RX.test(res)) return res.toLowerCase();
  if (/^\p{Lu}\p{Ll}+$/u.test(res)) {
    const stem = res.toLowerCase().slice(0, Math.max(3, res.length - 2));
    if (low.indexOf(" " + stem) < 0) return "rozm\xF3wca";
  }
  return res;
}
__name(txCleanRole, "txCleanRole");
__name2(txCleanRole, "txCleanRole");
function txCleanNames(namesJson, text) {
  let n = null;
  try {
    n = namesJson ? JSON.parse(namesJson) : null;
  } catch (e) {
    return null;
  }
  if (!n || typeof n !== "object") return null;
  let ch = false;
  const o = {};
  for (const k of Object.keys(n)) {
    const v = txCleanRole(n[k], text);
    if (v !== n[k]) ch = true;
    o[k] = v;
  }
  return ch ? JSON.stringify(o) : null;
}
__name(txCleanNames, "txCleanNames");
__name2(txCleanNames, "txCleanNames");
async function txAiSpeakers(env, words, category, lang, force, trainerAcc) {
  if (!env.AI) return { speakers: null, src: "ai-err", note: "brak Workers AI" };
  if (txIsMonolog(category) && !force) return { speakers: null, src: "ai-one", note: "monolog" };
  const sents = txSentences(words);
  if (sents.length < 2) return { speakers: null, src: "ai-one", note: "jedno zdanie" };
  const day = (/* @__PURE__ */ new Date()).toISOString().slice(0, 10);
  const u = await env.DB.prepare("SELECT calls FROM llm_usage WHERE day = ?").bind(day).first();
  if (u && u.calls >= txAiDailyLimit(env)) return { speakers: null, src: null, note: "dzienny limit AI wyczerpany" };
  const who = trainerAcc ? "Nagrywa TRENER programu Nowa Mowa \u2014 to jego rozmowa z kursantem (cz\u0119sto przez telefon). Trener prowadzi, dopytuje, chwali, daje wskaz\xF3wki o systemach mowy (zer\xF3wka, \u0142\u0105cznik, jedynka\u2026). Osoba 1 = trener, osoba 2 = kursant." : "Nagrywa kursant programu Nowa Mowa, kt\xF3ry \u0107wiczy codzienne rozmowy (sklep, telefon, urz\u0105d, ulica, praca). Zwykle zaczyna rozmow\u0119: wita si\u0119, pyta, prosi. Osoba 1 = kursant, osoba 2 = rozm\xF3wca (np. sprzedawczyni).";
  const sys = "Dzielisz zapis rozmowy (z rozpoznawania mowy, mog\u0105 by\u0107 b\u0142\u0119dy) na wypowiedzi os\xF3b. " + who + ' ZASADY: jedna osoba zwykle m\xF3wi kilka zda\u0144 z rz\u0119du \u2014 zmieniaj osob\u0119 tylko, gdy wynika to z sensu (pytanie \u2192 odpowied\u017A, reakcja na to, co powiedzia\u0142 rozm\xF3wca, kr\xF3tkie \u201EMhm\u201D, \u201ETak\u201D, \u201ENo\u201D, \u201EO, widzisz\u201D wtr\u0105cone w opowie\u015B\u0107 drugiej osoby, po\u017Cegnanie i odpowied\u017A na nie). Opowie\u015B\u0107 w 1. osobie (\u201Eja\u201D, \u201Eby\u0142am\u201D) ci\u0105gnie si\u0119 u tej samej osoby. Zwykle s\u0105 DOK\u0141ADNIE 2 osoby. Osoba 3 tylko wtedy, gdy wyra\u017Anie odzywa si\u0119 kto\u015B trzeci (np. kto\u015B w tle zwraca si\u0119 do rozm\xF3wc\xF3w) \u2014 nie u\u017Cywaj jej do \u201Eniepewnych\u201D zda\u0144. Odpowiedz WY\u0141\u0104CZNIE JSON-em: {"roles":{"1":"' + (trainerAcc ? "trener" : "kursant") + '","2":"kr\xF3tka rola, np. sprzedawczyni"},"zmiany":[[numer_zdania, osoba], \u2026]} \u2014 lista miejsc, w kt\xF3rych zaczyna m\xF3wi\u0107 kolejna osoba (pierwszy element dotyczy zdania 1). Imi\u0119 dopisz do roli TYLKO, gdy dok\u0142adnie pada w rozmowie (np. kto\u015B si\u0119 przedstawia) \u2014 nigdy go nie wymy\u015Blaj.';
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
  const allText = words.map((w) => w[0]).join(" ");
  for (const k of Object.keys(roles)) roles[k] = txCleanRole(roles[k], allText);
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
    const prio = Object.keys(await txPrioGet(env));
    if (prio.length) {
      const pr = (await pg.query(
        `SELECT r.id::text AS id, r.user_id::text AS user_id, r.record_file, u.country, COALESCE(rct.name, rc.name) AS category, ${TX_PATH_SQL_COLS} FROM records r
         LEFT JOIN users u ON u.id = r.user_id
         LEFT JOIN record_categories rc ON rc.id = r.record_category_id
         LEFT JOIN record_category_translations rct ON rct.record_category_id = rc.id AND rct.locale = 'pl'
         WHERE r.deleted_at IS NULL AND r.record_file IS NOT NULL AND r.user_id::text = ANY($1)
         ORDER BY r.created_at DESC LIMIT 3000`,
        [prio]
      )).rows;
      const seen = new Set(pr.map((r) => r.id));
      rows = pr.concat(rows.filter((r) => !seen.has(r.id)));
    }
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
  let phoneWait = /* @__PURE__ */ new Set();
  try {
    await env.DB.prepare("CREATE TABLE IF NOT EXISTS phone_pending (ns_record_id TEXT PRIMARY KEY, at INTEGER)").run();
    const pp = (await env.DB.prepare("SELECT ns_record_id FROM phone_pending WHERE at > ?").bind(nowS - 1800).all()).results || [];
    phoneWait = new Set(pp.map((x) => String(x.ns_record_id)));
    if (Math.random() < 0.05) await env.DB.prepare("DELETE FROM phone_pending WHERE at < ?").bind(nowS - 86400).run();
  } catch (e) {
  }
  const missing = changed.concat(rows.filter((r) => !state[r.id] || state[r.id].status === "error" && nowS - state[r.id].created_at > 6 * 3600)).filter((r) => !phoneWait.has(String(r.id)));
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
  try {
    const cur = await env.DB.prepare("SELECT v FROM tx_kv WHERE k = 'names_clean_v1'").first();
    if (!cur || cur.v !== "done") {
      const after = Number(cur && cur.v || 0);
      const rs = (await env.DB.prepare("SELECT rowid AS rid, ns_record_id, spk_names, text FROM record_transcript WHERE rowid > ? AND spk_names IS NOT NULL ORDER BY rowid LIMIT 400").bind(after).all()).results || [];
      const ups = [];
      for (const x of rs) {
        const nn = txCleanNames(x.spk_names, x.text);
        if (nn) ups.push(env.DB.prepare("UPDATE record_transcript SET spk_names = ? WHERE ns_record_id = ?").bind(nn, x.ns_record_id));
      }
      for (let i = 0; i < ups.length; i += 50) await env.DB.batch(ups.slice(i, i + 50));
      const next = rs.length < 400 ? "done" : String(rs[rs.length - 1].rid);
      await env.DB.prepare("INSERT INTO tx_kv (k, v) VALUES ('names_clean_v1', ?) ON CONFLICT(k) DO UPDATE SET v = excluded.v").bind(next).run();
    }
  } catch (e) {
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
  const mk = /* @__PURE__ */ __name(() => ({ total: 0, done: 0, old_method: 0, errors: 0, waiting: 0, phone: 0 }), "mk");
  const out = { d30: mk(), d90: mk() }, errs = {}, errSample = [];
  for (const r of rows) {
    const x = st[r.id];
    const isPhone = !!(x && x.status === "ok" && /^android-asr/.test(String(x.model || "")));
    const k = !x ? "waiting" : x.status === "ok" ? isPhone || String(x.model || "").endsWith(TX_VERSION) ? "done" : "old_method" : x.status === "error" ? "errors" : "waiting";
    for (const b of r.m30 ? [out.d30, out.d90] : [out.d90]) {
      b.total++;
      b[k]++;
      if (isPhone) b.phone++;
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
  let phonePending = 0;
  try {
    phonePending = (await env.DB.prepare("SELECT COUNT(*) AS n FROM phone_pending WHERE at > ?").bind(Math.floor(Date.now() / 1e3) - 1800).first() || {}).n || 0;
  } catch (e) {
  }
  return json({
    ok: true,
    ...out,
    phone_pending: phonePending,
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
    const rs = await env.DB.prepare(`SELECT ns_record_id, ns_student_id, text, lang, dur, words, speakers, spk_names, spk_src, spk_people, edited_by, summary, fixed, created_at, model FROM record_transcript WHERE status = 'ok' AND ns_record_id IN (${chunk.map(() => "?").join(",")})`).bind(...chunk).all();
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
      longest_pause: Math.round(longest * 10) / 10,
      model: r.model || null
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

