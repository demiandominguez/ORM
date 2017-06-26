package tp.utn;

import java.sql.Connection;
import java.util.List;

import org.junit.Assert;
import tp.utn.demo.domain.Direccion;
import tp.utn.demo.domain.Ocupacion;
import tp.utn.demo.domain.Persona;
import tp.utn.demo.domain.PersonaDireccion;
import tp.utn.demo.domain.TipoOcupacion;
import tp.utn.modelo.Ormsql;
import tp.utn.modelo.Utn;

public class Test
{
	@org.junit.Test
	public void testFind() throws Exception
	{
		ConnectDatabase conector = ConnectDatabase.getInstance();
		Connection con = conector.getConnection();

		// verifico el find
		Persona p = Utn.find(con,Persona.class,12);
		Assert.assertEquals(p.getNombre(),"Pablo");
		Assert.assertEquals((Integer)p.getOcupacion().getIdOcupacion(),(Integer)4);

		// ocupacion es LAZY => debe permanecer NULL hasta que haga el get
		Assert.assertNull(p.ocupacion);

		// debe traer el objeto
		Ocupacion o = p.getOcupacion();
		Assert.assertNotNull(o);

		// verifico que lo haya traido bien
		Assert.assertEquals(o.getDescripcion(),"Ingeniero");

		// tipoOcupacion (por default) es EAGER => no debe ser null
		Assert.assertNotNull(o.getTipoOcupacion());
		TipoOcupacion to = o.getTipoOcupacion();

		// verifico que venga bien...
		Assert.assertEquals(to.getDescripcion(),"Profesional");

		// -- Relation --

		// las relaciones son LAZY si o si!
		Assert.assertNull(p.direcciones);

		List<PersonaDireccion> dirs = p.getDirecciones();
		Assert.assertNotNull(dirs);

		// debe tener 2 elementos
		Assert.assertEquals(dirs.size(),2);

		for(PersonaDireccion pd:dirs)
		{
			Persona p1 = pd.getPersona();
			Direccion d = pd.getDireccion();

			Assert.assertNotNull(p1);
			Assert.assertNotNull(d);

			Assert.assertEquals(p1.getNombre(),p.getNombre());
		}

		List<Persona> personas = Utn.findAll(con,Persona.class);
		Assert.assertEquals(personas.size(),10);

		Persona pupdate = Utn.find(con,Persona.class,12);
		Assert.assertEquals(pupdate.getNombre(),"Pablo");
		String xql = "";
		pupdate.setNombre("Demian");
		int resultado = Utn.update(con,pupdate);
		pupdate = Utn.find(con,Persona.class,12);
		Assert.assertEquals("Demian", pupdate.getNombre());
		Assert.assertFalse("Pablo" == pupdate.getNombre());
		
		
	}
}
