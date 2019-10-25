package ch.fhnw.swc.mrs;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.stream.Stream;

import ch.fhnw.swc.mrs.data.SimpleMRSServices;
import ch.fhnw.swc.mrs.view.MainController;

/**
 * Main class of the Movie Rental System App.
 */
public class MovieRentalSystem {

    private SimpleMRSServices backend = new SimpleMRSServices();

    /**
     * start initialization of the application.
     */
    public void start() {
        backend.createDB();
        MainController controller = new MainController();
        controller.init(backend).setVisible(true);
    }

    /**
     * The main method to start the app.
     * 
     * @param args currently ignored.
     * @throws Exception whenever something goes wrong.
     */
    public static void main(String[] args) throws Exception {
        loadPriceCategories();
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new MovieRentalSystem().start();
            }
        });
    }

    private static void loadPriceCategories() throws Exception {
        URI uri = MovieRentalSystem.class.getClassLoader().getResource("data/pricecategories.config").toURI();
        initFileSystem(uri);
        try (Stream<String> stream = Files.lines(Paths.get(uri))) {
            stream.forEach(x -> {
                try {
                    Class.forName(x);
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            });
        }
    }
    
    // need to explicitly create a file system; otherwise will cause a FileSystemNotFoundException
    // when executing the file from command line.
    // see discussion on stackoverflow; here a short link: https://goo.gl/1RgnjW 
    private static FileSystem initFileSystem(URI uri) throws IOException {
        try {
            return FileSystems.newFileSystem(uri, Collections.emptyMap());
        } catch (IllegalArgumentException e) { 
            return FileSystems.getDefault();
        }
    }

}
