package tp.utn.demo.domain;

import tp.utn.ann.Column;
import tp.utn.ann.Id;
import tp.utn.ann.Table;

@Table(name="vivienda")
public class TipoVivienda
{
		@Id(strategy=Id.IDENTITY)
		@Column(name="id_tipovivienda")
		private Integer idVivienda;

		@Column(name="nombre")
		private String nombre;
}
