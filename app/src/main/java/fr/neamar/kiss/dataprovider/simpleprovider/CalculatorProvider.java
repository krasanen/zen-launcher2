package fr.neamar.kiss.dataprovider.simpleprovider;

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
        p = Pattern.compile("(-?)([0-9.]+)\\s?([+\\-*/×x÷])\\s?(-?)([0-9.]+)");
    }

    @Override
    public void requestResults(String query, Searcher searcher) {
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
            } catch (NullPointerException e){
                return;
            }


            SearchPojo pojo = new SearchPojo("calculator://", query +" = "+result, "", SearchPojo.CALCULATOR_QUERY);

            pojo.relevance = 100;
            searcher.addResult(pojo);
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
