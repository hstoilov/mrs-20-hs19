package ch.fhnw.swc.mrs.data;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import org.postgresql.ds.PGSimpleDataSource;
import org.sql2o.Sql2o;
import org.sql2o.converters.Converter;
import org.sql2o.converters.UUIDConverter;
import org.sql2o.quirks.PostgresQuirks;

import ch.fhnw.swc.mrs.api.MRSServices;
import ch.fhnw.swc.mrs.model.Movie;
import ch.fhnw.swc.mrs.model.PriceCategory;
import ch.fhnw.swc.mrs.model.Rental;
import ch.fhnw.swc.mrs.model.User;
import ch.fhnw.swc.mrs.util.LocalDateConverter;
import ch.fhnw.swc.mrs.util.PriceCategoryConverter;

public class DbMRSServices implements MRSServices {

    private PGSimpleDataSource datasource;
    private Sql2o sql2o;
    
    /**
     * A MRSServices facade for PostresqlDB is initialized according to the passed config.
     * @param config must contain the properties "url", "user" and "pwd"
     */
    public DbMRSServices(Properties config) {
        String serverName = config.getProperty("server");
        int portNumber = Integer.parseInt(config.getProperty("port"));
        String userName = config.getProperty("user");
        String password = config.getProperty("pwd");
        datasource = new PGSimpleDataSource();
        datasource.setServerName(serverName);
        datasource.setPortNumber(portNumber);
        datasource.setUser(userName);
        datasource.setPassword(password);
        
        // Important: do not forget to register special data types
        @SuppressWarnings("rawtypes")
        Map<Class, Converter> converters = new HashMap<>();
        converters.put(UUID.class, new UUIDConverter());
        converters.put(LocalDate.class, new LocalDateConverter());
        converters.put(PriceCategory.class, new PriceCategoryConverter());
        
        sql2o = new Sql2o(datasource, new PostgresQuirks(converters));
    }

    
    private MovieDAO getMovieDAO() { 
        return new MovieDAO(sql2o);
    }

    private UserDAO getUserDAO() { 
        return new UserDAO(sql2o);
    }

    private RentalDAO getRentalDAO() { 
        return new RentalDAO(sql2o);
    }

    @Override
    public Movie createMovie(String aTitle, LocalDate aReleaseDate, String aPriceCategory, int anAgeRating) {
        try {
            PriceCategory pc = PriceCategory.getPriceCategoryFromId(aPriceCategory);
            Movie m = new Movie(aTitle, aReleaseDate, pc, anAgeRating);
            getMovieDAO().saveOrUpdate(m);
            return m;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public List<Movie> getAllMovies() {
        return getMovieDAO().getAll();
    }

    @Override
    public List<Movie> getAllMovies(boolean rented) {
        return getMovieDAO().getAll(rented);
    }

    @Override
    public Movie getMovieById(UUID id) {
        return getMovieDAO().getById(id);
    }

    @Override
    public boolean updateMovie(Movie movie) {
        try {
            getMovieDAO().saveOrUpdate(movie);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean deleteMovie(UUID id) {
        try {
            getMovieDAO().delete(id);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public List<User> getAllUsers() {
        return getUserDAO().getAll();
    }

    @Override
    public User getUserById(UUID id) {
        return getUserDAO().getById(id);
    }

    @Override
    public User getUserByName(String name) {
        List<User> users = getUserDAO().getByName(name);
        return users.size() == 0 ? null : users.get(0);
    }

    @Override
    public User createUser(String aName, String aFirstName, LocalDate aBirthdate) {
        try {
            User u = new User(aName, aFirstName, aBirthdate);
            getUserDAO().saveOrUpdate(u);
            return u;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public boolean updateUser(User user) {
        try {
            getUserDAO().saveOrUpdate(user);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean deleteUser(UUID id) {
        try {
            getUserDAO().delete(id);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public List<Rental> getAllRentals() {
        return getRentalDAO().getAll();
    }

    @Override
    public boolean createRental(UUID userId, UUID movieId, LocalDate d) {
        // TODO: transaction is missing
        User u = getUserDAO().getById(userId);
        Movie m = getMovieDAO().getById(movieId);

        if (u != null && m != null && !m.isRented() && !d.isAfter(LocalDate.now())) {
            getRentalDAO().create(userId, movieId, d);
            m.setRented(true);
            getMovieDAO().saveOrUpdate(m);
            return true;
        }
        return false;
    }

    @Override
    public boolean returnRental(UUID id) {
        RentalDAO rdao = getRentalDAO();
        Rental r = rdao.getById(id);
        Movie m = r.getMovie();
        m.setRented(false);
        getMovieDAO().saveOrUpdate(m);
        rdao.delete(id);
        return r != null;
    }
    
    @Override
    public void createDB() {
        try (Connection conn = datasource.getConnection()) {
            Statement statement = conn.createStatement();
            statement.execute(createMoviesTable);
            statement.execute(createUsersTable);
            statement.execute(createRentalsTable);
        } catch (SQLException se) {
            se.printStackTrace();
        }        
    }
    
    @Override
    public void removeDB() {
        try (Connection conn = datasource.getConnection()) {
            Statement statement = conn.createStatement();
            statement.execute(dropRentalsTable);
            statement.execute(dropMoviesTable);
            statement.execute(dropUsersTable);
        } catch (SQLException se) {
            se.printStackTrace();
        }
    }

    private static final String createMoviesTable =
            "CREATE TABLE IF NOT EXISTS movies ( "
            + "MovieId uuid NOT NULL, "
            + "Title text NOT NULL, "
            + "Rented boolean NOT NULL, "
            + "ReleaseDate date NOT NULL, "
            + "PriceCategory text NOT NULL, "
            + "AgeRating integer NOT NULL, "
            + "CONSTRAINT movies_pkey PRIMARY KEY (MovieId)" 
            + ");";
    private static final String createUsersTable =
            "CREATE TABLE IF NOT EXISTS users ( "
            + "UserId uuid NOT NULL, "
            + "Name text NOT NULL, "
            + "FirstName text NOT NULL, "
            + "Birthdate date NOT NULL, "
            + "CONSTRAINT users_pkey PRIMARY KEY (UserId) "
            + ");";
    private static final String createRentalsTable =
            "CREATE TABLE IF NOT EXISTS rentals ( "
            + "RentalId uuid NOT NULL, "
            + "MovieId uuid NOT NULL, "
            + "UserId uuid NOT NULL, "
            + "RentalDate date NOT NULL, "
            + "CONSTRAINT rentals_pkey PRIMARY KEY (RentalId), "
            + "CONSTRAINT NoDuplicateRentals UNIQUE (MovieId, UserId), "
            + "CONSTRAINT movieFK FOREIGN KEY (MovieId) "
            + "    REFERENCES movies (MovieId) MATCH SIMPLE "
            + "    ON UPDATE NO ACTION "
            + "    ON DELETE NO ACTION, "
            + "CONSTRAINT userFK FOREIGN KEY (UserId) "
            + "    REFERENCES users (UserId) MATCH SIMPLE "
            + "    ON UPDATE NO ACTION "
            + "    ON DELETE NO ACTION "
            + ");";
    private static final String dropMoviesTable  = "DROP TABLE movies";
    private static final String dropUsersTable   = "DROP TABLE users";
    private static final String dropRentalsTable = "DROP TABLE rentals";
}
