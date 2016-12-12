/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mangaclient;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

/**
 * The MOR Engine for Manga Web Scraping with Manga APIs
 *
 * @author David Huynh
 */
public interface MorEngine {

    public String getCurrentURL();

    public void setCurrentURL(String url);

    public BufferedImage loadImg(String url) throws IOException;

    public BufferedImage getImage(String url) throws IOException;

    public String getNextPage();

    public String getPreviousPage();

    public boolean isValidPage(String url);

    public List<String> getMangaList();

    public String getMangaName();

    public String[] getChapterList();

    public String[] getPageList();

    public String[] getChapterNames();

    public String getMangaURL(String mangaName);

    public int getCurrentPageNum();

    public int getCurrentChapNum();
}
