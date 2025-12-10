package fr.insa.toto.webui;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.theme.Theme;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import fr.insa.toto.model.GestionBDD;
import fr.insa.toto.model.BdDTest;

@SpringBootApplication
@Theme("default")
public class Application extends SpringBootServletInitializer implements AppShellConfigurator {

    public static void main(String[] args) {
        // seulement si bdd h2 en memoire
        try (Connection con = ConnectionPool.getConnection()) {
            GestionBDD.razBdd(con);
            BdDTest.createBdDTestV2(con);
        } catch (SQLException ex) {
            throw new Error(ex);
        }
        SpringApplication.run(Application.class, args);
    }

}
