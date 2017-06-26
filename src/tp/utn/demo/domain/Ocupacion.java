package tp.utn.demo.domain;

import java.util.List;

import tp.utn.ann.Column;
import tp.utn.ann.Id;
import tp.utn.ann.Relation;
import tp.utn.ann.Table;

@Table(name="ocupacion")
public class Ocupacion
{
	@Id(strategy=Id.IDENTITY)
	@Column(name="id_ocupacion")
	private Integer idOcupacion;
	
	@Column(name="descripcion")
	private String descripcion;
	
	@Column(name="id_tipoocupacion")
	public TipoOcupacion tipoOcupacion;

	@Relation(mappedBy="ocupacion", type=Persona.class)
	public List<Persona> personas;
	
	public List<Persona> getPersonas()
	{
		return personas;
	}

	public void setPersonas(List<Persona> personas)
	{
		this.personas=personas;
	}

	public Integer getIdOcupacion()
	{
		return idOcupacion;
	}

	public void setIdOcupacion(Integer idOcupacion)
	{
		this.idOcupacion=idOcupacion;
	}

	public String getDescripcion()
	{
		return descripcion;
	}

	public void setDescripcion(String descripcion)
	{
		this.descripcion=descripcion;
	}

	public TipoOcupacion getTipoOcupacion()
	{
		return tipoOcupacion;
	}

	public void setTipoOcupacion(TipoOcupacion tipoOcupacion)
	{
		this.tipoOcupacion=tipoOcupacion;
	}

	@Override
	public String toString()
	{
		return getDescripcion();
	}

	@Override
	public boolean equals(Object o)
	{
		Ocupacion other = (Ocupacion)o;		
		return other.getIdOcupacion().equals(idOcupacion)
			&& other.getDescripcion().equals(getDescripcion())
			&& other.getTipoOcupacion().equals(getTipoOcupacion());
	}	
}
