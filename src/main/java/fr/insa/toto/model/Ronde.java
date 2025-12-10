 /*
Copyright 2000- Francois de Bertrand de Beuvron

This file is part of CoursBeuvron.

CoursBeuvron is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

CoursBeuvron is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with CoursBeuvron.  If not, see <http://www.gnu.org/licenses/>.
 */
package fr.insa.toto.model;

/**
 *
 * @author ThinkPad
 */

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import fr.insa.beuvron.utils.database.ClasseMiroir;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import fr.insa.toto.model.Match;

public class Ronde extends ClasseMiroir{

    private int numero;                       
    private LocalDateTime debut;
    private boolean close = false;            
    private List<Match> matchs = new ArrayList<>();

    public Ronde(int numero) {
        this.numero = numero;
        this.debut = LocalDateTime.now();
    }

    public int getNumero() { return numero; }
    public boolean isClose() { return close; }
    public void clore() { this.close = true; }

    public List<Match> getmatchs() { return matchs; }

    public void ajoutermatch(Match m) {
        this.matchs.add(m);
    }
    @Override
    protected PreparedStatement saveSansId(Connection con) throws SQLException {
        String sql = "INSERT INTO Ronde (numero, debut, close) VALUES (?, ?, ?)";
        PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

        ps.setInt(1, this.numero);
        ps.setTimestamp(2, java.sql.Timestamp.valueOf(this.debut));
        ps.setBoolean(3, this.close);
        

        return ps;
    }
}


