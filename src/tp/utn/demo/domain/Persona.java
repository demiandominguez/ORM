package tp.utn.demo.domain;

import java.util.List;

import tp.utn.ann.Column;
import tp.utn.ann.Id;
import tp.utn.ann.Relation;
import tp.utn.ann.Table;

@Table(name="persona")
public class Persona
{
	@Id(strategy=Id.IDENTITY)
	@Column(name="id_persona")
	private Integer idPersona;

	@Column(name="nombre")
	private String nombre;

	@Column(fetchType=Column.LAZY, name="id_ocupacion")
	public Ocupacion ocupacion;

	@Relation(mappedBy="persona", type=PersonaDireccion.class)
	public List<PersonaDireccion> direcciones;

	public List<PersonaDireccion> getDirecciones()
	{
		return direcciones;
	}

	public void setDirecciones(List<PersonaDireccion> direcciones)
	{
		this.direcciones=direcciones;
	}

	public Integer getIdPersona()
	{
		return idPersona;
	}

	public void setIdPersona(Integer idPersona)
	{
		this.idPersona=idPersona;
	}

	public String getNombre()
	{
		return nombre;
	}

	public void setNombre(String nombre)
	{
		this.nombre=nombre;
	}

	public Ocupacion getOcupacion()
	{
		return ocupacion;
	}

	public void setOcupacion(Ocupacion ocupacion)
	{
		this.ocupacion=ocupacion;
	}

	@Override
	public boolean equals(Object o)
	{
		Persona other = (Persona)o;
		return other.getIdPersona().equals(getIdPersona())
				&& other.getNombre().equals(getNombre())
				&& other.getOcupacion().equals(getOcupacion());
	}

	public String toString()
	{
		return getNombre();
	}
}
