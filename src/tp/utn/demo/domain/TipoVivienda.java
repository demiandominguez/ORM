package tp.utn.demo.domain;

import tp.utn.ann.Column;
import tp.utn.ann.Id;
import tp.utn.ann.Table;

@Table(name="tipo_vivienda")
public class TipoVivienda
{
		@Id(strategy=Id.IDENTITY)
		@Column(name="id_tipo_vivienda")
		private Integer idVivienda;

		@Column(name="descripcion")
		private String nombre;

		public Integer getIdVivienda()
		{
			return idVivienda;
		}

		public void setIdVivienda(Integer idVivienda)
		{
			this.idVivienda=idVivienda;
		}

		public String getNombre()
		{
			return nombre;
		}

		public void setNombre(String nombre)
		{
			this.nombre=nombre;
		}
		
		@Override
		public String toString()
		{
			return "TipoVivienda [idVivienda="+idVivienda+", nombre="+nombre+"]";
		}

		@Override
		public boolean equals(Object obj)
		{
			TipoVivienda other=(TipoVivienda)obj;
			if(nombre==null)
			{
				if(other.getNombre()!=null) return false;
			}
			else if(!nombre.equals(other.getNombre())) return false;
			if(idVivienda!=other.getIdVivienda()) return false;
			return true;
		}
}
