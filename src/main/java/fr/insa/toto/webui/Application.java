package fr.insa.toto.webui;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.theme.Theme;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

import fr.insa.toto.model.GestionBDD;
import fr.insa.toto.model.BdDTest;
import fr.insa.beuvron.utils.database.ConnectionPool;

import java.sql.Connection;
import java.sql.SQLException;

@SpringBootApplication
@Theme("default")
public class Application extends SpringBootServletInitializer implements AppShellConfigurator {

    public static void main(String[] args) {

        // ✅ Reset uniquement si tu lances avec -DRESET_DB=true
        // Ex: mvn spring-boot:run -Dspring-boot.run.jvmArguments="-DRESET_DB=true"
        // ou: java -DRESET_DB=true -jar tonapp.jar
        if ("true".equalsIgnoreCase(System.getProperty("RESET_DB"))) {
            try (Connection con = ConnectionPool.getConnection()) {
                System.out.println("⚠️ RESET_DB=true -> RAZ COMPLETE + données de test");
                GestionBDD.razBdd(con);
                BdDTest.createBdDTestV2(con);
            } catch (SQLException ex) {
                throw new Error(ex);
            }
        } else {
            System.out.println("✅ Pas de reset BDD (RESET_DB != true)");
        }

        SpringApplication.run(Application.class, args);
    }
}
