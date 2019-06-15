package fr.neamar.kiss.dataprovider.simpleprovider;

import android.annotation.TargetApi;
import android.os.Build;
import android.util.Log;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.neamar.kiss.BuildConfig;
import fr.neamar.kiss.pojo.SearchPojo;
import fr.neamar.kiss.searcher.Searcher;
import io.github.endreman0.calculator.Calculator;
import io.github.endreman0.calculator.expression.Variable;
import io.github.endreman0.calculator.expression.type.Type;

public class CalculatorProvider extends SimpleProvider {
    private static final String TAG = CalculatorProvider.class.getSimpleName();
    private Pattern p;

    public CalculatorProvider() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            p = Pattern.compile("(-?)([0-9.]+)\\s?([+\\-*/×x÷])\\s?(-?)([0-9.]+)");
        } else {
            p = Pattern.compile("^(-?)([0-9.]+)\\s?([+\\-*/×x÷])\\s?(-?)([0-9.]+)$");
        }
    }

    @Override
    public void requestResults(String query, Searcher searcher) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {

            // Now create matcher object.
            String result;
            Matcher m = p.matcher(query);
            if (m.find()) {
                // calculator lib needs spaces between operators
                query = query.replace("+", " + ");
                query = query.replace("*", " * ");
                query = query.replace("/", " / ");
                query = query.replace("-", " - ");
                if (BuildConfig.DEBUG) Log.d(TAG,"query="+query);
                try {
                    result = String.valueOf(Calculator.calculate(query));
                } catch (Exception e){
                    if (BuildConfig.DEBUG){
                        Log.d(TAG,"requestResults, exception:"+e);
                    }
                    return;
                }
                SearchPojo pojo = new SearchPojo("calculator://", query +" = "+result, "", SearchPojo.CALCULATOR_QUERY);
                pojo.relevance = 100;
                searcher.addResult(pojo);
            }



        } else {


            // pre android N (Fatal Exception: java.lang.NoSuchMethodError: No virtual method getAnnotationsByType(Ljava/lang/Class;)[Ljava/lang/annotation/Annotation; in class Ljava/lang/reflect/Field; or its super classes (declaration of 'java.lang.reflect.Field' appears in /system/framework/core-libart.jar)

            // Now create matcher object.
            Matcher m = p.matcher(query);
            if (m.find()) {
                String operator = m.group(3);

                // let's go for floating point arithmetic
                // we need to add a "0" on top of it to support ".2" => 0.2
                // For every other case, this doesn't change the number "01" => 1
                float lhs = Float.parseFloat("0" + m.group(2));
                lhs = m.group(1).equals("-") ? -lhs : lhs;
                float rhs = Float.parseFloat("0" + m.group(5));
                rhs = m.group(4).equals("-") ? -rhs : rhs;

                float floatResult = 0;
                switch (operator) {
                    case "+":
                        floatResult = lhs + rhs;
                        break;
                    case "-":
                        floatResult = lhs - rhs;
                        break;
                    case "*":
                    case "×":
                    case "x":
                        floatResult = lhs * rhs;
                        operator = "×";
                        break;
                    case "/":
                    case "÷":
                        floatResult = lhs / rhs;
                        operator = "÷";
                        break;
                    default:
                        floatResult = Float.POSITIVE_INFINITY;
                }

                String queryProcessed = floatToString(lhs) + " " + operator + " "
                        + floatToString(rhs) + " = " + floatToString(floatResult);
                SearchPojo pojo = new SearchPojo("calculator://", queryProcessed, "", SearchPojo.CALCULATOR_QUERY);

                pojo.relevance = 100;
                searcher.addResult(pojo);
            }
        }



    }

    private String floatToString(float f) {
        // If f is an int, we don't want to display 9.0: cast to int
        if (f == Math.round(f)) {
            return Integer.toString(Math.round(f));
        } else {
            // otherwise, keep it as float, knowing that some floating-point issues can happen
            // (try for instance 0.3 - 0.2)
            return Float.toString(f);
        }
    }
}
