package goat.util;

import static goat.util.TextFilters.scotchify;

import goat.core.Users;

import java.util.Map;

import com.memetix.mst.detect.Detect;
import com.memetix.mst.language.Language;
import com.memetix.mst.translate.Translate;

public class TranslateWrapper {

    {
        // it's kind of dumb that we have to auth both of these, but it seems that's how it works...
        Translate.setClientId(Passwords.getPassword("microsoft.clientId"));
        Translate.setClientSecret(Passwords.getPassword("microsoft.secret"));
        Detect.setClientId(Passwords.getPassword("microsoft.clientId"));
        Detect.setClientSecret(Passwords.getPassword("microsoft.secret"));
    }

    public static final Language DEFAULT_GOAT_LANGUAGE = Language.ENGLISH;

    public Language defaultLanguage() {
        return DEFAULT_GOAT_LANGUAGE;
    }

    public Language detect(String text) throws Exception {
        return Detect.execute(text);
    }

    public String translate(String text, Language from, Language to) throws Exception {
        return Translate.execute(text, from, to);
    }

    public String translate(String text, Language to) throws Exception {
        return Translate.execute(text, to);
    }

    public String transloop(String text, String loopLangId) throws Exception {
        Language looplang = languageFromString(loopLangId);
        String tmp = Translate.execute(text, DEFAULT_GOAT_LANGUAGE, looplang);
        return Translate.execute(tmp, looplang, DEFAULT_GOAT_LANGUAGE);
    }

    // I can't believe we have to do this
    public Language languageFromString(String str) throws Exception {
        Map<String, Language> langs = Language.values(DEFAULT_GOAT_LANGUAGE);
        Language ret = null;
        if (langs.containsKey(str))
            ret = langs.get(str);
        if (ret == null)
            for(Language l: langs.values())
                if (l.toString().equals(str)) {
                    ret = l;
                    break;
                }
        if (ret == null)
            for(Language l: langs.values())
                if (l.name().toLowerCase().equals(str.replaceAll("\\s", "_"))) {
                    ret = l;
                    break;
                }
        if (ret == null && str.length() > 3)
            for(Language l: langs.values())
                if (l.name().toLowerCase().startsWith(str)) {
                    ret = l;
                    break;
                }
        return ret;
    }

    private static Users users = goat.Goat.getUsers();
    public String localize(Message m, String text) {
        if ( users.hasUser(m.getSender()) &&
             users.getUser(m.getSender()).getWeatherStation().startsWith("EGP"))
            return scotchify(text);
        else
            return text;
    }
}
