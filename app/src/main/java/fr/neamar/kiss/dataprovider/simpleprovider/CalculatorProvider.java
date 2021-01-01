package fr.neamar.kiss.dataprovider.simpleprovider;


import android.util.Log;

import org.kobjects.expressionparser.ExpressionParser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fi.zmengames.zen.Calculator;
import fr.neamar.kiss.BuildConfig;
import fr.neamar.kiss.pojo.SearchPojo;
import fr.neamar.kiss.searcher.Searcher;


public class CalculatorProvider extends SimpleProvider {
    private static final String TAG = CalculatorProvider.class.getSimpleName();
    private final Pattern p;

    public CalculatorProvider() {
        p = Pattern.compile("(-?)([0-9.]+)\\s?([+\\-*/×x÷])\\s?(-?)([0-9.]+)");
    }

    @Override
    public void requestResults(String query, Searcher searcher) {
        // Now create matcher object.
        if (BuildConfig.DEBUG) Log.i(TAG, "requestResults");
        String result;
        Matcher m = p.matcher(query);
        if (m.find()) {
            try {
            if (BuildConfig.DEBUG) Log.i(TAG, "query=" + query);
                ExpressionParser<Double> parser = Calculator.DoubleProcessor.createParser();
                result = String.valueOf(parser.parse(query));
            } catch (Exception e) {
                if (BuildConfig.DEBUG)  Log.i(TAG, "requestResults, exception:" + e);
                return;
            }
            SearchPojo pojo = new SearchPojo("calculator://", query + " = " + result, "", SearchPojo.CALCULATOR_QUERY);
            pojo.relevance = 100;
            searcher.addResult(pojo);

        }
    }
}
