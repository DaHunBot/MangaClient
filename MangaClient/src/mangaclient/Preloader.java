/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mangaclient;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;

/**
 *
 * @author David Huynh
 */
public class Preloader implements MorEngine, Closeable {

    private MorEngine morEngine;//Current MangaEngine
    private BufferedImage[] pages;//Current Images
    private String[] pageURLs; //URLs corresponding to current images
    private String mangaName;//CurrentMangaName

    private JProgressBar progressBar;//The Progress Monitor
    private Container parent; //The Parent Component to display the loading bar in
    private Task task;//The Swing Worker that handles repaint

    /**
     * Constructor
     *
     * @param window The JFrame you want to add the progressbar to
     * @param mangaEngine The Engine you want to prefetch from
     */
    public Preloader(Container window, MorEngine mangaEngine) {
        this.morEngine = mangaEngine;
        parent = window;
        mangaName = mangaEngine.getMangaName();
        pageURLs = mangaEngine.getPageList();
        progressBar = new JProgressBar(0, mangaEngine.getPageList().length);
        preload();
    }

    /**
     * Performs the prefetching
     */
    public void preload() {
        mangaName = morEngine.getMangaName();
        pageURLs = morEngine.getPageList();
        pages = new BufferedImage[pageURLs.length];
        progressBar.setValue(0);
        progressBar.setMaximum(pageURLs.length);
        progressBar.setStringPainted(true);
        if (task != null && !task.isDone() && !task.isCancelled()) {//Cancels previous task before starting a new one.
            task.cancel(true);
        }
        task = new Task();
        task.addPropertyChangeListener(new PropertyChangeListener() {
            /**
             * Invoked when a task's progress property changes.
             */
            @Override
            public void propertyChange(PropertyChangeEvent evt) {//Updates Progressbar
                if ("progress".equals(evt.getPropertyName())) {
                    int progress = (Integer) evt.getNewValue();
                    parent.repaint();
                    progressBar.setValue(progress);
                    progressBar.setString("Loading Page: " + progress + " of " + progressBar.getMaximum());
                }
            }
        });
        task.execute();
    }

    /**
     * Checks solely whether or not the URL is in the database.
     *
     * @param URL The URL you want to check
     * @return True if in database false otherwise.
     */
    private boolean isCached(String URL) {
        for (int i = 0; i < pageURLs.length; i++) {
            if (pageURLs[i].equals(URL) && pages[i] != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if url is in database.
     *
     * @param URL The URL You want to check
     * @return True if the URL is found, false otherwise.
     */
    private boolean isLoaded(String URL) {
        for (int i = 0; i < pageURLs.length; i++) {
            if (pageURLs[i].equals(URL)) {
                return true;
            }
        }
        System.out.println("Prefetching because of " + URL);
        return false;
    }

    /**
     * Fetches the data
     *
     * @param URL
     * @return
     */
    private BufferedImage fetch(String URL) {
        if (!isCached(URL) || morEngine.getCurrentPageNum() > pages.length) {
            try {
                BufferedImage icon = morEngine.loadImg(URL);
                if (!isLoaded(URL)) {
                    preload();
                }
                if (icon == null) {//Sometimes the chapter ends or starts on a blank page.
                    icon = morEngine.loadImg(morEngine.getNextPage());
                }
                return icon;
            } catch (IOException e) {
                Logger.log(e);
                return null;
            }
        } else {
            return pages[morEngine.getCurrentPageNum() - 1];
        }
    }

    @Override
    public String getCurrentURL() {
        return morEngine.getCurrentURL();
    }

    @Override
    public void setCurrentURL(String url) {
        morEngine.setCurrentURL(url);
    }

    @Override
    public BufferedImage loadImg(String url) throws IOException {
        if (url == null) {
            return null;
        }
        if (isCached(url)) {
            morEngine.setCurrentURL(url);
            return fetch(url);
        } else {
            BufferedImage out = morEngine.loadImg(url);
            if (!isLoaded(url)) {
                preload();
            }
            return out;
        }
    }

    @Override
    public BufferedImage getImage(String url) throws IOException {
        return morEngine.getImage(url);
    }

    @Override
    public String getNextPage() {
        String currentURL = morEngine.getCurrentURL();
        String nextPage = morEngine.getNextPage();
        if (isCached(nextPage) || task == null || task.isCancelled() || task.isDone()) {
            return nextPage;
        } else {
            return currentURL;
        }
    }

    @Override
    public String getPreviousPage() {
        return morEngine.getPreviousPage();
    }

    @Override
    public boolean isValidPage(String url) {
        return morEngine.isValidPage(url);
    }

    @Override
    public List<String> getMangaList() {
        return morEngine.getMangaList();
    }

    @Override
    public String getMangaName() {
        return mangaName;
    }

    @Override
    public String[] getChapterList() {
        return morEngine.getChapterList();
    }

    @Override
    public String[] getPageList() {
        return pageURLs;
    }

    @Override
    public String getMangaURL(String mangaName) {
        return morEngine.getMangaURL(mangaName);
    }

    /**
     * Returns the MangaEngine that the class wraps
     *
     * @return The wrapped MangaEngine
     */
    public MorEngine getMangaEngine() {
        return morEngine;
    }

    @Override
    public int getCurrentPageNum() {
        return morEngine.getCurrentPageNum();
    }

    @Override
    public int getCurrentChapNum() {
        return morEngine.getCurrentChapNum();
    }

    @Override
    public String[] getChapterNames() {
        return morEngine.getChapterNames();
    }

    @Override
    public void close() {
        if (task != null && !task.isDone() && !task.isCancelled()) {//Cancels previous task before starting a new one.
            task.cancel(true);
        }
    }

    /**
     * Where the actual prefetching happens
     *
     */
    class Task extends SwingWorker<Void, Void> {

        public Task() {
            parent.add(progressBar, BorderLayout.SOUTH);//Adds ProgressBar to bottom
            parent.revalidate();//Refreshes JFRame
            parent.repaint();
        }

        /**
         * Main task. Executed in background thread.
         */
        @Override
        public Void doInBackground() {
            for (int i = 0; i < pageURLs.length && !this.isCancelled(); i++) {
                int attemptNum = 0;
                while (attemptNum <= 3) {//Retries three times to load the image.
                    try {
                        pages[i] = morEngine.getImage(pageURLs[i]);//Loads image
                        progressBar.setValue(i);//Updates progressbar
                        progressBar.setString("Loading Page: " + (i + 1) + " of " + progressBar.getMaximum());
                        break;
                    } catch (IOException e) {
                        Logger.log(e);
                        attemptNum++;
                    }
                }
            }
            return null;
        }

        /**
         * Executed in event dispatching thread
         */
        @Override
        public void done() {
            super.done(); //Cleans up
            parent.remove(progressBar); //Removes progressbar
            parent.revalidate(); //Refreshes JFrame
            parent.repaint();
        }

    }
}
