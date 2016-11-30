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
        if (url == null || url.equals("")) {
            return null;
        }

        Document doc = Jsoup.connect(url).timeout(5 * 1000).get();
        Element e = doc.getElementById("image");
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

        if (index + 1 == pages.length) {
            return getNextChapter();
        } else {
            return pages[index + 1];
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
        Document doc;
        try {
            doc = Jsoup.connect(currentURL).get();
            Element e = doc.getElementsByClass("prew_page").first();
            String backPage = e.absUrl("href");

            //Special Case: Beginning of Chapter
            if (backPage == null || backPage.equals("") || backPage.equals("javascript:void(0)")) {
                backPage = getPreviousChapter(doc);
            }

            return backPage;
        } catch (IOException e) {
            Logger.log(e);
        }
        return null;
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

    //@Override
    public String getMangaName(String url) {
        String manga = url.substring(url.lastIndexOf("/manga/") + 7);

        while (manga.indexOf('/') > -1) {
            manga = manga.substring(0, manga.indexOf('/'));
        }
        return manga;
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
            boolean found = false;
            for (int i = 0; !found && i < mangaList[0].length; i++) {
                String name = mangaList[0][i];
                if (mangaName.equalsIgnoreCase(name)) {
                    mangaURL = mangaList[1][i];
                    found = true;
                }
            }

            if (!found) {
                mangaURL = searchForManga(mangaName);
            }

            mangaURL = getFirstChapter(mangaURL);
        } catch (IOException e) {
            Logger.log(e);
            mangaName = StringUtil.removeTrailingWhiteSpaces(mangaName);
            mangaURL = mangaNameToURL(mangaName);
            mangaURL = MANGAREADER_URL + mangaURL;
        }

        return mangaURL;
    }

    /**
     * **********************************************************************************************
     */
    private String searchForManga(String searchTerm) throws IOException {
        String url = "http://www.mangareader.net/search";
        String encoded = URLEncoder.encode(searchTerm, "UTF-8");
        url += "?name=" + encoded;
        Document doc = Jsoup.connect(url).timeout(5000).get();
        Elements results = doc.getElementsByClass("result_search").first().children();
        results.remove(results.last());//Removes useless link from footer

        for (Element e : results) {
            String text = e.children().last().text();
            text = text.substring(text.indexOf(':') + 1);
            String[] names = text.split(";");

            for (String s : names) {
                if (s.substring(1).equalsIgnoreCase(searchTerm)) {
                    return e.select("a").first().absUrl("href");
                }
            }
        }

        return "";
    }

    private String getFirstChapter(String mangaURL) throws IOException {
        Document doc = Jsoup.connect(mangaURL).get();
        Element e = doc.getElementsByClass("detail_list").last();
        Element item = e.select("a").last();
        return item.absUrl("href");
    }

    @Override
    public int getCurrentPageNum() {
        if (currentURL.charAt(currentURL.length() - 1) == '/') {
            return 1;
        } else {
            String page = currentURL.substring(currentURL.lastIndexOf('/') + 1, currentURL.lastIndexOf('.'));
            return Integer.parseInt(page);
        }
    }

    @Override
    public int getCurrentChapNum() {
        return (int) getChapNum(currentURL);
    }

    private double getChapNum(String url) {
        if (!StringUtil.containsNum(url) || url.lastIndexOf('c') == -1) {
            return -1;
        }

        url = url.substring(url.lastIndexOf('c'));
        url = url.substring(1, url.indexOf('/'));
        return Double.parseDouble(url);//Rounds to an int. Needed for v2 uploads and such.
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
        String[][] out;
        Document doc = Jsoup.connect("http://www.mangareader.net/alphabetical").timeout(10 * 1000).maxBodySize(0).get();
        Elements items = doc.getElementsByClass("manga_info");
        items = MangaUtil.removeLicensedManga(items);
        out = new String[2][items.size()];

        for (int i = 0; i < items.size(); i++) {
            Element item = items.get(i);
            out[0][i] = item.text();
            out[1][i] = item.absUrl("href");
        }

        return out;
    }

    private String[] intializeChapterList() {
        try {
            String linkPage = StringUtil.urlToText(currentURL);
            String url = "http://www.mangareader.net/get_chapters" + parseSeriesID(linkPage)
                    + ".js?v=306";
            String page = StringUtil.urlToText(url);
            page = page.substring(page.indexOf("Array(") + 6);
            page = page.substring(0, page.indexOf(");"));
            page = page.substring(1, page.length() - 1);
            String[] array = page.split("\\],\n  \\[");
            String name = parseSeriesName(linkPage);

            for (int i = 0; i < array.length; i++) {
                String s = array[i];
                s = StringUtil.stripQuotes(s);
                s = s.substring(s.indexOf("http"));
                s = s.replace("\"+series_name+\"", name);
                array[i] = s;
            }

            return array;
        } catch (IOException e) {
            Logger.log(e);
        }

        return null;
    }

    private String[] initializeChapterNames() {
        try {
            String linkPage = StringUtil.urlToText(currentURL);
            String url = "http://www.mangareader.net/get_chapters" + parseSeriesID(linkPage)
                    + ".js?v=306";
            String page = StringUtil.urlToText(url);
            page = page.substring(page.indexOf("Array(") + 6);
            page = page.substring(0, page.indexOf(");"));
            page = page.substring(1, page.length() - 1);
            String[] array = page.split("\\],\n  \\[");

            for (int i = 0; i < array.length; i++) {
                String s = array[i];
                s = StringUtil.stripQuotes(s);

                if (s.indexOf(':') != -1) {
                    s = s.substring(0, s.indexOf(':'));
                }

                if (s.indexOf('"') != -1) {
                    s = s.substring(0, s.indexOf('"'));
                }

                s = s.substring(s.lastIndexOf(' ') + 1);
                s = StringUtil.formatChapterNames(s);
                array[i] = s;
            }

            return array;
        } catch (IOException e) {
            Logger.log(e);
        }

        return null;
    }

    private String[] initalizePageList() {
        List<String> pages = new ArrayList<>();

        try {
            Document doc = Jsoup.connect(currentURL).get();
            Element list = doc.getElementsByClass("wid60").first();

            for (Element item : list.children()) {
                pages.add(item.attr("value"));
            }
        } catch (IOException e) {
            Logger.log(e);
        }

        String[] out = new String[pages.size()];
        pages.toArray(out);
        return out;
    }

    /**
     * **********************************************************************************************
     */
    private String parseSeriesName(String page) {
        page = page.substring(page.indexOf("series_name = \"") + 15);
        page = page.substring(0, page.indexOf('"'));
        return page;
    }

    private String parseSeriesID(String page) {
        page = page.substring(page.indexOf("series_id") + 10);
        page = page.substring(0, page.indexOf('&'));
        return page;
    }

    private String mangaNameToURL(String name) {
        name = name.toLowerCase();

        while (name.charAt(name.length() - 1) == ' ') {
            //Removes trailing whitespaces
            name = name.substring(0, name.length() - 1);
        }

        name = name.replace('@', 'a');//Special Case
        name = name.replace("& ", "");//Special Case
        name = name.replace(' ', '_');
        name = name.replace(",", "");

        for (int i = 0; i < name.length(); i++) {
            char x = name.charAt(i);

            if (!Character.isLetter(x) && x != '_' && !Character.isDigit(x)) {
                if (i - 1 >= 0 && i + 1 >= name.length() && name.charAt(i - 1) == '_' && name.charAt(i + 1) == '_') {
                    name = name.replace("" + x, "");
                } else {
                    name = name.replace(x, '_');
                }
            }

        }
        return name;
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
