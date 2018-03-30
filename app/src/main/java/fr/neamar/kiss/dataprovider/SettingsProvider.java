package fi.zmengames.zlauncher.dataprovider;

import java.util.Locale;

import fi.zmengames.zlauncher.R;
import fi.zmengames.zlauncher.loader.LoadSettingsPojos;
import fi.zmengames.zlauncher.normalizer.StringNormalizer;
import fi.zmengames.zlauncher.pojo.SettingsPojo;
import fi.zmengames.zlauncher.searcher.Searcher;
import fi.zmengames.zlauncher.utils.FuzzyScore;

public class SettingsProvider extends Provider<SettingsPojo> {
    private String settingName;

    @Override
    public void reload() {
        super.reload();
        this.initialize(new LoadSettingsPojos(this));

        settingName = this.getString(R.string.settings_prefix).toLowerCase(Locale.ROOT);
    }

    @Override
    public void requestResults(String query, Searcher searcher) {
        StringNormalizer.Result queryNormalized = StringNormalizer.normalizeWithResult(query, false);

        if (queryNormalized.codePoints.length == 0) {
            return;
        }

        FuzzyScore fuzzyScore = new FuzzyScore(queryNormalized.codePoints);
        FuzzyScore.MatchInfo matchInfo = new FuzzyScore.MatchInfo();

        for (SettingsPojo pojo : pojos) {
            boolean match = fuzzyScore.match(pojo.normalizedName.codePoints, matchInfo);
            pojo.relevance = matchInfo.score;

            if (match) {
                pojo.setNameHighlight(matchInfo.getMatchedSequences());
            } else if (fuzzyScore.match(settingName, matchInfo)) {
                match = true;
                pojo.relevance = matchInfo.score;
                pojo.clearNameHighlight();
            }

            if (match) {
                if (!searcher.addResult(pojo))
                    return;
            }
        }
    }
}
