package tp.utn;

import tp.utn.demo.domain.Direccion;
import tp.utn.demo.domain.Persona;
import tp.utn.modelo.Utn;

public class main
{
	public static void main (String[] args){
		System.out.println(
				Utn._query(Persona.class, "$id_persona = -8")
		);
		System.out.println(
				Utn._query(Direccion.class, null)
		);
	}
}
