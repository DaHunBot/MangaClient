/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mangaclient;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.imageio.ImageIO;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * Unofficial API for MangaReader.net
 *
 * @author David Huynh
 */
public class MangaReaderAPI implements MorEngine {
    //Saves Manga Url for future uses.

    private String currentURL;

    //MangaHere API Variables
    private final static String MANGAREADER_URL = "http://www.mangareader.net/";
    private final String[][] mangaList;

    private String[] pageURL;
    private String[] chapterURL;
    private String[] chapterName;

    /*
    * Setting the URL to start at from MangaReader
    * Throws if MOR cannot connect to MangaReader.com
     */
    public MangaReaderAPI(String URL) throws IOException {
        this.currentURL = URL;
        mangaList = initMangaList();
        refresh();
    }

    /*
    * Default Page: Horimiya
     */
    public MangaReaderAPI() throws IOException {
        this("http://www.mangareader.net/horimiya/1");//Default Manga         
    }

    /**
     * **********************************************************************************************
     */
    /*
    * Getter/Setter Method for currentURL
     */
    @Override
    public String getCurrentURL() {
        return currentURL;
    }

    @Override
    public void setCurrentURL(String url) {
        this.currentURL = url;
    }

    /**
     * **********************************************************************************************
     */
    /*
    * 
     */
    @Override
    public BufferedImage loadImg(String url) throws IOException {
        if (url == null || url.equals("")) {
            System.out.println(url);
            throw new IOException("INVALID PARAMETER");
        }

        //Checks to see a Manga is being loaded
        boolean hasMangaChanged = !getMangaName(currentURL).equals(getMangaName(url));
        boolean hasChapterChanged = getCurrentChapNum() != getChapNum(url);
        BufferedImage image = getImage(url);
        currentURL = url;

        if (hasMangaChanged || hasChapterChanged) {
            //Refreshes Chapter & Page URLs
            refresh();
        }
        return image;
    }

    @Override
    public BufferedImage getImage(String url) throws IOException {
        Document doc = Jsoup.connect(url).timeout(5 * 1000).get();
        Element e = doc.getElementById("img");
        String imgUrl = e.absUrl("src");
        return ImageIO.read(new URL(imgUrl));
    }

    /**
     * **********************************************************************************************
     */
    /*
    * This Next Section consists of uses
    * Will be able to do Next Page/Chapter
    * Will also be able to go back to a Previous Chapter/Page
     */
    @Override
    public String getNextPage() {
        String[] pages = this.getPageList();
        int index = StringUtil.indexOf(pages, currentURL);
        if (index + 1 < pages.length) {
            return pages[index + 1];
        }
        try {
            Document doc = Jsoup.connect(currentURL).get();
            Element navi = doc.getElementById("navi");
            String html = navi.getElementsByClass("next").html();//Manual Parsing Required
            html = html.substring(html.indexOf('"') + 2, html.lastIndexOf('"'));
            return MANGAREADER_URL + html;
        } catch (IOException e) {
            Logger.log(e);
            return null;
        }
    }

    public String getNextChapter() {
        try {
            return getNextChapter(Jsoup.connect(currentURL).get());
        } catch (IOException e) {
            Logger.log(e);
            return null;
        }
    }

    private String getNextChapter(Document doc) {
        Elements exs = doc.getElementsByClass("reader_tip").first().children();
        Element ex = exs.get(exs.size() - 2);
        String extract = ex.html();//Element is not properly closed. Manually parsing required
        if (!extract.contains("href=\"")) {
            //First Chapter: Special Case
            ex = exs.get(exs.size() - 1);
            extract = ex.html();
        }

        //Manually extracts the HREF
        extract = extract.substring(extract.indexOf("href=\""));
        extract = extract.substring(extract.indexOf('"') + 1);
        extract = extract.substring(0, extract.indexOf('"'));

        //TODO Update parsing with Regex
        return extract;
    }

    @Override
    public String getPreviousPage() {
        try {
            Document doc = Jsoup.connect(currentURL).get();
            Element navi = doc.getElementById("navi");
            String html = navi.getElementsByClass("prev").html();//Manual Parsing Required
            if (html.contains("href=\"\"")) {//Signifies that page does not exist
                return currentURL;
            }
            html = html.substring(html.indexOf('"') + 2, html.lastIndexOf('"'));
            return MANGAREADER_URL + html;
        } catch (IOException e) {
            Logger.log(e);
            return null;
        }
    }

    public String getPreviousChapter() {
        try {
            return getPreviousChapter(Jsoup.connect(currentURL).get());
        } catch (IOException e) {
            Logger.log(e);
            return null;
        }
    }

    private String getPreviousChapter(Document doc) {
        Elements exs = doc.getElementsByClass("reader_tip").first().children();
        Element ex = exs.get(exs.size() - 1);
        String extract = ex.html();//Element not closed properly. Manual parsing required.
        extract = extract.substring(extract.indexOf("href=\""));
        extract = extract.substring(extract.indexOf('"') + 1);
        extract = extract.substring(0, extract.indexOf('"'));
        return extract;
    }

    /*
    * Checks to see if there the page url is still live
     */
    @Override
    public boolean isValidPage(String url) {
        Document doc;
        try {
            doc = Jsoup.connect(url).get();
            return !(doc.hasClass("error_404") || doc.hasClass("mangaread_error")
                    || doc.text().contains(" is not available yet. We will update")
                    || this.isMangaLicensed(url));
        } catch (IOException e) {
            Logger.log(e);
            return false;
        }
    }

    /**
     * **********************************************************************************************
     */
    @Override
    public List<String> getMangaList() {
        List<String> names = new ArrayList<>(mangaList[0].length);
        names.addAll(Arrays.asList(mangaList[0]));
        return names;
    }

    @Override
    public String getMangaName() {
        return getMangaName(currentURL);
    }

    public String getMangaName(String url) {
        String name = url.replace(MANGAREADER_URL, "");
        name = name.substring(0, name.indexOf("/"));
        if (isMangaHash(name)) {//If it's a MangaHash, we have to parse the next / 
            name = url.replace(MANGAREADER_URL, "");
            name = name.substring(0, name.lastIndexOf('/'));
            name = name.substring(name.lastIndexOf("/") + 1);
        }
        name = name.replace('-', ' ');
        assert (!name.contains("."));
        return name;
    }

    @Override
    public String[] getChapterList() {
        return chapterURL;
    }

    @Override
    public String[] getPageList() {
        return pageURL;
    }

    @Override
    public String[] getChapterNames() {
        return chapterName;
    }

    @Override
    public String getMangaURL(String mangaName) {
        String mangaURL = "";
        mangaName = StringUtil.removeTrailingWhiteSpaces(mangaName);
        try {
            for (int i = 0; i < mangaList[0].length; i++) {
                String name = mangaList[0][i];
                if (mangaName.equalsIgnoreCase(name)) {
                    mangaURL = mangaList[1][i];
                    break;
                }
            }
            mangaURL = getFirstChapter(mangaURL);
            System.out.println(mangaURL);
        } catch (IOException e) {
            Logger.log(e);
        }
        return mangaURL;
    }

    /**
     * **********************************************************************************************
     */
    private String getFirstChapter(String mangaURL) throws IOException {
        Document doc = Jsoup.connect(mangaURL).timeout(10 * 1000).get();
        Element list = doc.getElementById("listing");
        Elements names = list.select("a");
        return names.first().absUrl("href");
    }

    @Override
    public int getCurrentPageNum() {
        String check = currentURL.replace(MANGAREADER_URL, "");
        check = check.substring(0, check.indexOf('/'));
        if (isMangaHash(check)) {//There are two types of ways of delineating Manga on the site
            String page = check.substring(check.lastIndexOf('-') + 1);
            return (int) Double.parseDouble(page);
        } else {
            String number = currentURL.substring(currentURL.lastIndexOf('/'));
            return (int) Double.parseDouble(number);
        }
    }

    @Override
    public int getCurrentChapNum() {
        return (int) getChapNum(currentURL);
    }

    private double getChapNum(String url) {
        if (url.contains("chapter")) {
            String test = url.substring(url.lastIndexOf('-') + 1);
            if (test.indexOf('.') != -1) {
                test = test.substring(0, test.indexOf('.'));
            }
            return Double.parseDouble(test);
        }
        if (hasMangaHash(url)) {//There are two types of ways of delineating Manga on the site
            String chapter = url.replace(MANGAREADER_URL, "");
            chapter = chapter.substring(0, chapter.indexOf("/"));
            chapter = chapter.substring(chapter.indexOf('-') + 1, chapter.lastIndexOf('-'));
            return Double.parseDouble(chapter);
        } else {
            String number = url.substring(0, url.lastIndexOf('/'));
            number = number.substring(number.lastIndexOf('/') + 1);
            if (!StringUtil.isNum(number)) {//In case it grabs the name instead
                number = url.substring(url.lastIndexOf('/') + 1);
            }
            return Double.parseDouble(number);
        }
    }

    public boolean isMangaLicensed(String mangaURL) throws IOException {
        Document doc;
        doc = Jsoup.connect(mangaURL).get();
        Element e = doc.getElementsByClass("detail_list").first();

        if (e == null) {
            return false;
        }

        return e.text().contains("has been licensed, it is not available in MangaHere.");
    }

    private String[][] initMangaList() throws IOException {
        try {
            Document doc = Jsoup.connect("http://www.mangareader.net/alphabetical").timeout(10 * 1000)
                    .maxBodySize(0).get();
            Elements bigList = doc.getElementsByClass("series_alpha");
            Elements names = new Elements();
            for (Element miniList : bigList) {
                names.addAll(miniList.select("li"));
            }
            String[][] localMangaList = new String[2][names.size()];
            for (int i = 0; i < names.size(); i++) {
                Element e = names.get(i).select("a").first();
                localMangaList[0][i] = e.text().replace("[Completed]", "");
                localMangaList[1][i] = e.absUrl("href");
            }
            return localMangaList;
        } catch (IOException e) {
            Logger.log(e);
            return null;
        }

    }

    private String[] intializeChapterList() {
        String baseURL = MANGAREADER_URL + getMangaName().replace(' ', '-');
        try {
            List<String> outList = new ArrayList<String>();
            Document doc = Jsoup.connect(baseURL).timeout(10 * 1000).get();
            Element list = doc.getElementById("listing");
            Elements names = list.select("tr");
            names = names.select("a");
            for (Element e : names) {
                outList.add(e.absUrl("href"));
            }
            String[] out = new String[outList.size()];
            outList.toArray(out);
            return out;
        } catch (IOException e) {
            Logger.log(e);
            return null;
        }
    }

    private String[] initializeChapterNames() {
        String baseURL = MANGAREADER_URL + getMangaName().replace(' ', '-');
        try {
            Document doc = Jsoup.connect(baseURL).timeout(10 * 1000).maxBodySize(0).get();
            Element list = doc.getElementById("listing");
            Elements names = list.select("tr");
            names = names.select("a");
            String[] out = new String[names.size()];
            for (int i = 0; i < out.length; i++) {
                out[i] = names.get(i).text();
            }
            out = StringUtil.formatChapterNames(out);
            return out;
        } catch (IOException e) {
            Logger.log(e);
            return null;
        }
    }

    private String[] initalizePageList() {
        try {
            Document doc = Jsoup.connect(currentURL).timeout(10 * 1000).get();
            Elements items = doc.getElementById("pageMenu").children();
            String[] out = new String[items.size()];
            for (int i = 0; i < items.size(); i++) {
                out[i] = items.get(i).absUrl("value");
            }
            return out;
        } catch (IOException e) {
            Logger.log(e);
        }
        return null;
    }

    /**
     * **********************************************************************************************
     */
    /**
     * Checks if it's uses a manga hash to store data about the manga.
     *
     * @param input The String you want to check
     * @return True if it is, false otherwise
     */
    private boolean isMangaHash(String input) {
        for (char c : input.toCharArray()) {
            if (!(Character.isDigit(c) || c == '-')) {
                return false;
            }
        }
        return input.contains("-"); // In case it got the chapter number at the end
    }

    private boolean hasMangaHash(String URL) {
        String end = URL.replace(MANGAREADER_URL, "");
        String[] pieces = end.split("/");
        for (String s : pieces) {
            if (isMangaHash(s)) {
                return true;
            }
        }
        return false;
    }

    /**
     * **********************************************************************************************
     */
    private void refresh() {
        chapterURL = this.intializeChapterList();
        pageURL = this.initalizePageList();
        chapterName = this.initializeChapterNames();
    }

}
