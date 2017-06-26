package tp.utn;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import tp.utn.demo.domain.Ocupacion;
import tp.utn.demo.domain.Persona;
import tp.utn.demo.domain.PersonaDireccion;
import tp.utn.modelo.Utn;

public class main
{
	public static void main (String[] args){	
		ConnectDatabase conector = ConnectDatabase.getInstance();
		Connection con = conector.getConnection();
		String xql = "@nombre like '?'";
		Persona p = new Persona();
		p.setNombre("Pablo");

		try
		{
			/*
			List<Persona> newPersona = Utn.query(con, Persona.class, xql, p.getNombre());
			System.out.println(newPersona.size());
			System.out.println(newPersona.get(0));
			System.out.println(newPersona.get(0).getIdPersona());
			System.out.println("pase el prin persona");
			System.out.println(newPersona.get(0).getOcupacion());
			System.out.println("pase el lazy....proximo es relation");
			List<PersonaDireccion> personaDirecciones = newPersona.get(0).getDirecciones();
			for(PersonaDireccion pd:personaDirecciones){
				System.out.println(pd.getDireccion());
			}
			*/
			Persona pablo = Utn.find(con,Persona.class,12);
			//Ocupacion ocupacion = Utn.find(con,Ocupacion.class,5);
			//pablo.setOcupacion(ocupacion);
			//pablo.setNombre("Demian");
			//Utn.update(con,pablo);
			
			Ocupacion ocupacion = Utn.find(con,Ocupacion.class,5);
			p.setOcupacion(ocupacion);
			p.setNombre("Demian");
			//Utn.insert(con,p);
			System.out.println(Utn.delete(con,Persona.class,22));
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally{
			if( con!=null ) try
			{
				con.close();
			}
			catch(SQLException e)
			{
				e.printStackTrace();
			}
		}
	}
}
