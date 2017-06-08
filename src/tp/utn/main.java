package tp.utn;

import java.sql.Connection;
import java.util.List;

import tp.utn.demo.domain.Direccion;
import tp.utn.demo.domain.Persona;
import tp.utn.modelo.Utn;

public class main
{
	public static void main (String[] args){	
		ConnectDatabase conector = new ConnectDatabase();
		Connection con = conector.getConnection();
		String xql = "@nombre like '?'";
		Persona p = new Persona();
		p.setNombre("demian");

		try
		{
			List<Persona> newPersona = Utn.query(con, Persona.class, xql, p.getNombre());
			System.out.println(newPersona.get(0));
			System.out.println(newPersona.get(0).getOcupacion());
			System.out.println(newPersona.get(0).getDireccion().getPersonas());
			System.out.println(newPersona.get(0));
			if( con!=null ) con.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
}
