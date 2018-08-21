(ns axel-f.function-test
  (:require #?(:clj [clojure.test :as t]
               :cljs [cljs.test :as t :include-macros true])
            [axel-f.core :as sut]))

(t/deftest function-test

  (t/testing "function arguments parsed as vector"

    (t/is (vector? (last (sut/compile "SUM()"))))
    (t/is (empty? (last (sut/compile "SUM()"))))

    (t/is (= [1 2 3]
             (last (sut/compile "SUM(1,2,3)"))
             (last (sut/compile "SUM(1, 2, 3)"))
             (last (sut/compile "SUM( 1, 2,3 )")))))

  (t/testing "nested function calls"

    (let [[fncall & _] (last (sut/compile "SUM(SUM(1,2,3),5)") )]
      (t/is (= [:FNCALL "SUM" [1 2 3]]
               fncall))))

  (t/testing "vectors can be passed as arguments"

    (t/is (= [:FNCALL "SUM" [[:VECTOR 1 2 3] 4]]
             (sut/compile "SUM({1,2,3}, 4)")))

    (t/is (= 10 (sut/run "SUM({1,2,3},4)"))
          (= 15 (sut/run "SUM({1,2,3}, {4,5})")))))

(t/deftest ROUND

  (t/testing "ROUND function should work as expected"

    (t/are [x y] (t/is (= x (sut/run (str "ROUND(123123.123, " y ")"))))
      123000     -3
      123100     -2
      123120     -1
      123123     0
      123123.1   1
      123123.12  2
      123123.123 3)

    (t/is (= 123123 (sut/run "ROUND(123123.123)")))))

(t/deftest COUNT

  (t/testing "COUNT function should work as expected"

    (t/are [x y] (t/is (= x (sut/run (str "COUNT(" y ")") {:foo [1 2 3]})))
      0 ""
      0 "{}"
      1 "{1}"
      3 "foo")))

(t/deftest MIN

  (t/testing "MIN function should work as expected"

    (t/are [x y] (t/is (= x (sut/run (str "MIN(" y ")") {:foo [1 2 3]})))
      1 "{1}"
      1 "foo")))

(t/deftest MAX

  (t/testing "MAX function should work as expected"

    (t/are [x y] (t/is (= x (sut/run (str "MAX(" y ")") {:foo [1 2 3]})))
      1 "{1}"
      3 "foo")))

(t/deftest CONCATENATE

  (t/testing "CONCATENATE function should work as expected"

    (t/are [x y] (t/is (= x (sut/run (str "CONCATENATE(" y ")") {:foo [1 2 3]})))
      "1" "{1}"
      "2" "foo[1]"
      "123" "foo")))

(t/deftest IF

  (t/testing "IF function should work as expected"

    (t/are [x y] (t/is (= x (sut/run (str "IF(" y ")") {:foo [1 2 3]})))
      0 "FALSE, 1, 0"
      1 "TRUE, 1, 0"
      1 "True, 1"
      nil "False, 1")))

(t/deftest AVERAGE

  (t/testing "AVERAGE function should work as expected"

    (t/are [x y] (t/is (= x (sut/run (str "AVERAGE(" y ")") {:foo [1 2 3] :bar []})))
      2 "{1,2,3}"
      nil "bar"
      2 "foo")))

(t/deftest AND

  (t/testing "AND function should work as expected"

    (t/are [x y] (t/is (= x (sut/run (str "AND(" y ")") {:foo [1 2 3] :bar []})))
      true "1 > 0, 2 > 1"
      false "1 > 0, 2 < 1"
      true "COUNT(foo)"
      false "AVERAGE(foo) < COUNT(bar)")))

(t/deftest OR

  (t/testing "OR function should work as expected"

    (t/are [x y] (t/is (= x (sut/run (str "OR(" y ")") {:foo [1 2 3] :bar []})))
      true "1 > 0, 2 > 1"
      true "1 > 0, 2 < 1"
      true "COUNT(foo)"
      false "AVERAGE(foo) < COUNT(bar)")))

(t/deftest OBJREF

  (t/testing "OBJREF function should work as expected"

    (t/are [x y] (t/is (= x (sut/run (str "OBJREF(" y ")") {:foo [1 2 3] :bar []})))
      2 "\"foo\", 1"
      [] "\"bar\""
      3 "CONCATENATE(\"f\", \"o\", \"o\"), 2"
      nil "\"baz\"")))

(def all-functions
  ["ABS"
   "ACCRINT"
   "ACCRINTM"
   "ACOS"
   "ACOSH"
   "ACOT"
   "ACOTH"
   "AGGREGATE"
   "ADDRESS"
   "AMORDEGRC"
   "AMORLINC"
   "AND"
   "ARABIC"
   "AREAS"
   "ASC"
   "ASIN"
   "ASINH"
   "ATAN"
   "ATAN2"
   "ATANH"
   "AVEDEV"
   "AVERAGE"
   "AVERAGEA"
   "AVERAGEIF"
   "AVERAGEIFS"
   "BAHTTEXT"
   "BASE"
   "BESSELI"
   "BESSELJ"
   "BESSELK"
   "BESSELY"
   "BETADIST"
   "BETA.DIST"
   "BETAINV"
   "BETA.INV"
   "BIN2DEC"
   "BIN2HEX"
   "BIN2OCT"
   "BINOMDIST"
   "BINOM.DIST"
   "BINOM.DIST.RANGE"
   "BINOM.INV"
   "BITAND"
   "BITLSHIFT"
   "BITOR"
   "BITRSHIFT"
   "BITXOR"
   "CALL"
   "CEILING"
   "CEILING.MATH"
   "CEILING.PRECISE"
   "CELL"
   "CHAR"
   "CHIDIST"
   "CHIINV"
   "CHITEST"
   "CHISQ.DIST"
   "CHISQ.DIST.RT"
   "CHISQ.INV"
   "CHISQ.INV.RT"
   "CHISQ.TEST"
   "CHOOSE"
   "CLEAN"
   "CODE"
   "COLUMN"
   "COLUMNS"
   "COMBIN"
   "COMBINA"
   "COMPLEX"
   "CONCAT"
   "CONCATENATE"
   "CONFIDENCE"
   "CONFIDENCE.NORM"
   "CONFIDENCE.T"
   "CONVERT"
   "CORREL"
   "COS"
   "COSH"
   "COT"
   "COTH"
   "COUNT"
   "COUNTA"
   "COUNTBLANK"
   "COUNTIF"
   "COUNTIFS"
   "COUPDAYBS"
   "COUPDAYS"
   "COUPDAYSNC"
   "COUPNCD"
   "COUPNUM"
   "COUPPCD"
   "COVAR"
   "COVARIANCE.P"
   "COVARIANCE.S"
   "CRITBINOM"
   "CSC"
   "CSCH"
   "CUBEKPIMEMBER"
   "CUBEMEMBER"
   "CUBEMEMBERPROPERTY"
   "CUBERANKEDMEMBER"
   "CUBESET"
   "CUBESETCOUNT"
   "CUBEVALUE"
   "CUMIPMT"
   "CUMPRINC"
   "DATE"
   "DATEDIF"
   "DATEVALUE"
   "DAVERAGE"
   "DAY"
   "DAYS"
   "DAYS360"
   "DB"
   "DBCS"
   "DCOUNT"
   "DCOUNTA"
   "DDB"
   "DEC2BIN"
   "DEC2HEX"
   "DEC2OCT"
   "DECIMAL"
   "DEGREES"
   "DELTA"
   "DEVSQ"
   "DGET"
   "DISC"
   "DMAX"
   "DMIN"
   "DOLLAR"
   "DOLLARDE"
   "DOLLARFR"
   "DPRODUCT"
   "DSTDEV"
   "DSTDEVP"
   "DSUM"
   "DURATION"
   "DVAR"
   "DVARP"
   "EDATE"
   "EFFECT"
   "ENCODEURL"
   "EOMONTH"
   "ERF"
   "ERF.PRECISE"
   "ERFC"
   "ERFC.PRECISE"
   "ERROR.TYPE"
   "EUROCONVERT"
   "EVEN"
   "EXACT"
   "EXP"
   "EXPON.DIST"
   "EXPONDIST"
   "FACT"
   "FACTDOUBLE"
   "FALSE"
   "F.DIST"
   "FDIST"
   "F.DIST.RT"
   "FILTERXML"
   "FIND"
   "F.INV"
   "F.INV.RT"
   "FINV"
   "FISHER"
   "FISHERINV"
   "FIXED"
   "FLOOR"
   "FLOOR.MATH"
   "FLOOR.PRECISE"
   "FORECAST"
   "Forecasting"
   "FORMULATEXT"
   "FREQUENCY"
   "F.TEST"
   "FTEST"
   "FV"
   "FVSCHEDULE"
   "GAMMA"
   "GAMMA.DIST"
   "GAMMADIST"
   "GAMMA.INV"
   "GAMMAINV"
   "GAMMALN"
   "GAMMALN.PRECISE"
   "GAUSS"
   "GCD"
   "GEOMEAN"
   "GESTEP"
   "GETPIVOTDATA"
   "GROWTH"
   "HARMEAN"
   "HEX2BIN"
   "HEX2DEC"
   "HEX2OCT"
   "HLOOKUP"
   "HOUR"
   "HYPERLINK"
   "HYPGEOM.DIST"
   "HYPGEOMDIST"
   "IF"
   "IFERROR"
   "IFNA"
   "IFS"
   "IMABS"
   "IMAGINARY"
   "IMARGUMENT"
   "IMCONJUGATE"
   "IMCOS"
   "IMCOSH"
   "IMCOT"
   "IMCSC"
   "IMCSCH"
   "IMDIV"
   "IMEXP"
   "IMLN"
   "IMLOG10"
   "IMLOG2"
   "IMPOWER"
   "IMPRODUCT"
   "IMREAL"
   "IMSEC"
   "IMSECH"
   "IMSIN"
   "IMSINH"
   "IMSQRT"
   "IMSUB"
   "IMSUM"
   "IMTAN"
   "INDEX"
   "INDIRECT"
   "INFO"
   "INT"
   "INTERCEPT"
   "INTRATE"
   "IPMT"
   "IRR"
   "ISBLANK"
   "ISERR"
   "ISERROR"
   "ISEVEN"
   "ISFORMULA"
   "ISLOGICAL"
   "ISNA"
   "ISNONTEXT"
   "ISNUMBER"
   "ISODD"
   "ISREF"
   "ISTEXT"
   "ISO.CEILING"
   "ISOWEEKNUM"
   "ISPMT"
   "JIS"
   "KURT"
   "LARGE"
   "LCM"
   "LEFT"
   "LEN"
   "LINEST"
   "LN"
   "LOG"
   "LOG10"
   "LOGEST"
   "LOGINV"
   "LOGNORM.DIST"
   "LOGNORMDIST"
   "LOGNORM.INV"
   "LOOKUP"
   "LOWER"
   "MATCH"
   "MAX"
   "MAXA"
   "MAXIFS"
   "MDETERM"
   "MDURATION"
   "MEDIAN"
   "MID"
   "MIN"
   "MINIFS"
   "MINA"
   "MINUTE"
   "MINVERSE"
   "MIRR"
   "MMULT"
   "MOD"
   "MODE"
   "MODE.MULT"
   "MODE.SNGL"
   "MONTH"
   "MROUND"
   "MULTINOMIAL"
   "MUNIT"
   "N"
   "NA"
   "NEGBINOM.DIST"
   "NEGBINOMDIST"
   "NETWORKDAYS"
   "NETWORKDAYS.INTL"
   "NOMINAL"
   "NORM.DIST"
   "NORMDIST"
   "NORMINV"
   "NORM.INV"
   "NORM.S.DIST"
   "NORMSDIST"
   "NORM.S.INV"
   "NORMSINV"
   "NOT"
   "NOW"
   "NPER"
   "NPV"
   "NUMBERVALUE"
   "OCT2BIN"
   "OCT2DEC"
   "OCT2HEX"
   "ODD"
   "ODDFPRICE"
   "ODDFYIELD"
   "ODDLPRICE"
   "ODDLYIELD"
   "OFFSET"
   "OR"
   "PDURATION"
   "PEARSON"
   "PERCENTILE.EXC"
   "PERCENTILE.INC"
   "PERCENTILE"
   "PERCENTRANK.EXC"
   "PERCENTRANK.INC"
   "PERCENTRANK"
   "PERMUT"
   "PERMUTATIONA"
   "PHI"
   "PHONETIC"
   "PI"
   "PMT"
   "POISSON.DIST"
   "POISSON"
   "POWER"
   "PPMT"
   "PRICE"
   "PRICEDISC"
   "PRICEMAT"
   "PROB"
   "PRODUCT"
   "PROPER"
   "PV"
   "QUARTILE"
   "QUARTILE.EXC"
   "QUARTILE.INC"
   "QUOTIENT"
   "RADIANS"
   "RAND"
   "RANDBETWEEN"
   "RANK.AVG"
   "RANK.EQ"
   "RANK"
   "RATE"
   "RECEIVED"
   "REGISTER.ID"
   "REPLACE"
   "REPT"
   "RIGHT"
   "ROMAN"
   "ROUND"
   "ROUNDDOWN"
   "ROUNDUP"
   "ROW"
   "ROWS"
   "RRI"
   "RSQ"
   "RTD"
   "SEARCH"
   "SEC"
   "SECH"
   "SECOND"
   "SERIESSUM"
   "SHEET"
   "SHEETS"
   "SIGN"
   "SIN"
   "SINH"
   "SKEW"
   "SKEW.P"
   "SLN"
   "SLOPE"
   "SMALL"
   "SQL.REQUEST"
   "SQRT"
   "SQRTPI"
   "STANDARDIZE"
   "STDEV"
   "STDEV.P"
   "STDEV.S"
   "STDEVA"
   "STDEVP"
   "STDEVPA"
   "STEYX"
   "SUBSTITUTE"
   "SUBTOTAL"
   "SUM"
   "SUMIF"
   "SUMIFS"
   "SUMPRODUCT"
   "SUMSQ"
   "SUMX2MY2"
   "SUMX2PY2"
   "SUMXMY2"
   "SWITCH"
   "SYD"
   "T"
   "TAN"
   "TANH"
   "TBILLEQ"
   "TBILLPRICE"
   "TBILLYIELD"
   "T.DIST"
   "T.DIST.2T"
   "T.DIST.RT"
   "TDIST"
   "TEXT"
   "TEXTJOIN"
   "TIME"
   "TIMEVALUE"
   "T.INV"
   "T.INV.2T"
   "TINV"
   "TODAY"
   "TRANSPOSE"
   "TREND"
   "TRIM"
   "TRIMMEAN"
   "TRUE"
   "TRUNC"
   "T.TEST"
   "TTEST"
   "TYPE"
   "UNICHAR"
   "UNICODE"
   "UPPER"
   "VALUE"
   "VAR"
   "VAR.P"
   "VAR.S"
   "VARA"
   "VARP"
   "VARPA"
   "VDB"
   "VLOOKUP"
   "WEBSERVICE"
   "WEEKDAY"
   "WEEKNUM"
   "WEIBULL"
   "WEIBULL.DIST"
   "WORKDAY"
   "WORKDAY.INTL"
   "XIRR"
   "XNPV"
   "XOR"
   "YEAR"
   "YEARFRAC"
   "YIELD"
   "YIELDDISC"
   "YIELDMAT"
   "Z.TEST"
   "ZTEST"])


(t/deftest text-function-test

  (t/testing "CLEAN function"
    (t/is (= true
             (sut/run "EXACT(\"Hello world\", \"Hello world\")"))))

  (t/testing "CODE function"
    (t/is (= 65
             (sut/run "CODE(\"A\")")))
    (t/is (= 1000
             (sut/run "CODE(\"Ϩ\")")))
    )

  (t/testing "CONCATENATE function"
    (t/is (= "hello world"
             (sut/run "CONCATENATE(\"hello\", \" \", \"world\")")))
    (t/is (= "hello world"
             (sut/run "CONCATENATE({\"hello\", \" \", \"world\"})")))
    (t/is (= "1hello"
             (sut/run "CONCATENATE(1, \"hello\",)")))
    (t/is (= "TRUEyes"
             (sut/run "CONCATENATE(true, \"yes\")")))
    (t/is (= "FALSEno"
             (sut/run "CONCATENATE(false, \"no\")")))
    )

  (t/testing "EXACT function"
    (t/is (= true
             (sut/run "EXACT(\"yes\", \"yes\" )")))
    )

  (t/testing "FIND function"
    (let [context {:data {:name "Miriam McGovern"}}]
      (t/is (= 1
               (sut/run "FIND(\"M\", data.name)" context)))
      (t/is (= 6
               (sut/run "FIND(\"m\", data.name)" context)))
      (t/is (= 8
               (sut/run "FIND(\"M\", data.name, 3)" context))))

    )
  )
