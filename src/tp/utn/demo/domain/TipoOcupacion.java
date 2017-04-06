package tp.utn.demo.domain;

import tp.utn.ann.Column;
import tp.utn.ann.Id;
import tp.utn.ann.Table;

@Table(name="tipo_ocupacion")
public class TipoOcupacion
{
	@Id(strategy=Id.IDENTITY)
	@Column(name="id_tipoocupacion")
	private Integer idTipoOcupacion;

	@Column(name="descripcion")
	private String descripcion;
	
	public Integer getIdTipoOcupacion()
	{
		return idTipoOcupacion;
	}

	public void setIdTipoOcupacion(Integer idTipoOcupacion)
	{
		this.idTipoOcupacion=idTipoOcupacion;
	}

	public String getDescripcion()
	{
		return descripcion;
	}

	public void setDescripcion(String descripcion)
	{
		this.descripcion=descripcion;
	}

	@Override
	public String toString()
	{
		return getDescripcion();
	}

	@Override
	public boolean equals(Object obj)
	{
		TipoOcupacion other=(TipoOcupacion)obj;
		if(descripcion==null)
		{
			if(other.getDescripcion()!=null) return false;
		}
		else if(!descripcion.equals(other.getDescripcion())) return false;
		if(idTipoOcupacion!=other.getIdTipoOcupacion()) return false;
		return true;
	}
	
	
}
