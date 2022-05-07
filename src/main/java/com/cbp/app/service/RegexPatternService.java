package com.cbp.app.service;

import org.springframework.stereotype.Service;
import java.util.regex.Pattern;

@Service
public class RegexPatternService {
    public static final Pattern urlMissingWwwPattern = Pattern.compile("^\\w+\\.\\w+");
    public static final Pattern localLinkPattern = Pattern.compile("^[^\\.]*\\/.+"); // maybe [a-z] instead of .
    public static final Pattern localPageLinkPattern = Pattern.compile("[^\\.]*\\.(?:" +
        "htm" +
        "|html" +
        "|xhtml" +
        "|phtml" +
        "|shtml" +
        "|php" +
        "|asp" +
        "|aspx" +
        "|jsp" +
        "|cfm" +
        ")$");
    public static final Pattern queryStringPattern = Pattern.compile("(.*)\\?.*");
    public static final Pattern asteriskStringPattern = Pattern.compile("(.*)\\*.*");
    public static final Pattern dateStringPattern = Pattern.compile("^[^\\.]*[0-9]+\\.[0-9]+\\.[0-9]+[^\\.]*$");
    public static final Pattern phoneStringPattern = Pattern.compile("^[0-9\\.]+$");
    public static final Pattern ipStringPattern = Pattern.compile("^(?:(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9][0-9]|[0-9])(\\.(?!$)|[:/]?)){4}");
    public static final Pattern anchorStringPattern = Pattern.compile("(.*)#.*");
    public static final Pattern alphanumericContentPattern = Pattern.compile("^([\\W_]*)|([\\W_]*$)");
    public static final Pattern subPagePattern = Pattern.compile("(.*?\\.\\w+)/.*");
    public static final Pattern nonWebResourcePattern = Pattern.compile(".*\\.(?:" +
        "bmp" +
        "|jpg" +
        "|jpeg" +
        "|png" +
        "|gif" +
        "|svg" +
        "|pdf" +
        "|doc" +
        "|docx" +
        "|xls" +
        "|xlsx" +
        "|ppt" +
        "|pptx" +
        "|ashx" +
        "|xml" +
        "|m3u" +
        "|pls" +
        "|zip" +
        "|rar" +
        "|rtf" +
        "|ods" +
        "|avi" +
        "|mp3" +
        "|mp4" +
        "|rss" +
        "|atom" +
        "|txt" +
        "|tif" +
        "|exe" +
        "|css" +
        "|json" +
        "|ico" +
        ")$");
    public static final Pattern nonWebProtocolPattern = Pattern.compile("^(" +
        "mailto" +
        "|mail" +
        "|skype" +
        "|whatsapp" +
        "|javascript" +
        "|file" +
        "|data" +
        "|mms" +
        "|ts3server" +
        "|steam" +
        "|dchub" +
        "|tel" +
        "|fax" +
        "|webcal" +
        "|callto" +
        "|viber" +
        "|samp" +
        "|ymsgr" +
        "|irc" +
        "|nav" +
        "):.*");
    public static final Pattern domesticWebsitePattern = Pattern.compile(".+\\.ro.*");
    public static final Pattern domesticNewsWebsitePattern = Pattern.compile(
        "(?:.*\\.)?adevarul\\.ro.*" +
        "|(?:.*\\.)?stirileprotv\\.ro.*" +
        "|(?:.*\\.)?libertatea\\.ro.*" +
        "|(?:.*\\.)?digi24\\.ro.*" +
        "|(?:.*\\.)?a1\\.ro.*" +
        "|(?:.*\\.)?antena3\\.ro.*" +
        "|(?:.*\\.)?cancan\\.ro.*" +
        "|(?:.*\\.)?realitatea\\.net.*" +
        "|(?:.*\\.)?romaniatv\\.net.*" +
        "|(?:.*\\.)?unica\\.net.*" +
        "|(?:.*\\.)?evz\\.ro.*" +
        "|(?:.*\\.)?gsp\\.ro.*" +
        "|(?:.*\\.)?click\\.ro.*" +
        "|(?:.*\\.)?csid\\.ro.*" +
        "|(?:.*\\.)?sfatulmedicului\\.ro.*" +
        "|(?:.*\\.)?digisport\\.ro.*" +
        "|(?:.*\\.)?ziare\\.com.*" +
        "|(?:.*\\.)?sport\\.ro.*" +
        "|(?:.*\\.)?stiripesurse\\.ro.*" +
        "|(?:.*\\.)?hotnews\\.ro.*" +
        "|(?:.*\\.)?mediafax\\.ro.*" +
        "|(?:.*\\.)?spynews\\.ro.*" +
        "|(?:.*\\.)?avocatnet\\.ro.*" +
        "|(?:.*\\.)?teotrandafir\\.com.*" +
        "|(?:.*\\.)?wowbiz\\.ro.*" +
        "|(?:.*\\.)?protv\\.ro.*" +
        "|(?:.*\\.)?zf\\.ro.*" +
        "|(?:.*\\.)?prosport\\.ro.*" +
        "|(?:.*\\.)?unica\\.ro.*" +
        "|(?:.*\\.)?kudika\\.ro.*" +
        "|(?:.*\\.)?ziaruldeiasi\\.ro.*" +
        "|(?:.*\\.)?gandul\\.ro.*" +
        "|(?:.*\\.)?gandul\\.info.*" +
        "|(?:.*\\.)?dcnews\\.ro.*" +
        "|(?:.*\\.)?bzi\\.ro.*" +
        "|(?:.*\\.)?ziaristii\\.com.*" +
        "|(?:.*\\.)?stiridecluj\\.ro.*" +
        "|(?:.*\\.)?cugetliber\\.ro.*" +
        "|(?:.*\\.)?ziuact\\.ro.*" +
        "|(?:.*\\.)?ebihoreanul\\.ro.*" +
        "|(?:.*\\.)?ziarulevenimentul\\.ro.*" +
        "|(?:.*\\.)?ziarulunirea\\.ro.*" +
        "|(?:.*\\.)?stiridiaspora\\.ro.*" +
        "|(?:.*\\.)?telegrafonline\\.ro.*" +
        "|(?:.*\\.)?bugetul\\.ro.*" +
        "|(?:.*\\.)?monitorulcj\\.ro.*" +
        "|(?:.*\\.)?replicaonline\\.ro.*" +
        "|(?:.*\\.)?aradon\\.ro.*" +
        "|(?:.*\\.)?bihon\\.ro.*" +
        "|(?:.*\\.)?tion\\.ro.*" +
        "|(?:.*\\.)?viata-libera\\.ro.*" +
        "|(?:.*\\.)?ziardesuceava\\.ro.*" +
        "|(?:.*\\.)?alba24\\.ro.*" +
        "|(?:.*\\.)?notabn\\.ro.*" +
        "|(?:.*\\.)?activenews\\.ro.*" +
        "|(?:.*\\.)?turnulsfatului\\.ro.*" +
        "|(?:.*\\.)?pandurul\\.ro.*" +
        "|(?:.*\\.)?obiectivbr\\.ro.*" +
        "|(?:.*\\.)?botosaneanul\\.ro.*" +
        "|(?:.*\\.)?vremeanoua\\.ro.*" +
        "|(?:.*\\.)?ziuadevest\\.ro.*" +
        "|(?:.*\\.)?monitoruldevrancea\\.ro.*" +
        "|(?:.*\\.)?financiarul\\.ro.*" +
        "|(?:.*\\.)?zi-de-zi\\.ro.*" +
        "|(?:.*\\.)?gorjeanul\\.ro.*" +
        "|(?:.*\\.)?emaramures\\.ro.*" +
        "|(?:.*\\.)?ziarulargesul\\.ro.*" +
        "|(?:.*\\.)?bzc\\.ro.*" +
        "|(?:.*\\.)?newsteam\\.ro.*" +
        "|(?:.*\\.)?vdtonline\\.ro.*" +
        "|(?:.*\\.)?ziar\\.com.*" +
        "|(?:.*\\.)?aktual24\\.ro.*" +
        "|(?:.*\\.)?tribuna\\.ro.*"
        );
    public static final Pattern socialMediaWebsitePattern = Pattern.compile(
        "(?:.*\\.)?facebook\\.com.*" +
        "|(?:.*\\.)?fb\\.com.*" +
        "|(?:.*\\.)?twitter\\.com.*" +
        "|(?:.*\\.)?instagram\\.com.*" +
        "|(?:.*\\.)?last\\.fm.*" +
        "|(?:.*\\.)?pinterest\\.com.*" +
        "|(?:.*\\.)?linkedin\\.com.*" +
        "|(?:.*\\.)?youtube\\.com.*" +
        "|(?:.*\\.)?gravatar\\.com.*"
    );
    public static final Pattern miscIgnorablePattern = Pattern.compile(
        "(?:.*\\.)?schoolloop\\.com.*" +
        "(?:.*\\.)?adperfect\\.com.*"
    );
    public static final Pattern indexingServicePattern = Pattern.compile(
        ".*google\\.com.*" +
        "^google\\..*" +
        "|.*alexa\\.com.*" +
        "|.*apple\\.com.*" +
        "|.*blogger\\.com.*" +
        "|.*trustpilot\\.com.*" +
        "|.*wordpress\\.com.*" +
        "|.*outlook\\.com.*" +
        "|.*blogspot\\.com.*" +
        "|.*blogspot\\.ro.*" +
        "|.*pe-harta\\.ro.*" +
        "|.*nym\\.ro.*" +
        "|.*archive\\.org.*" +
        "|.*creativecommons\\.org.*" +
        "|.*webstatsdomain\\.org.*" +
        "|.*webstatsdomain\\.com.*" +
        "|.*gov\\.uk.*"
    );
}
