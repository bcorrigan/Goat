package goat.util;
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
}
