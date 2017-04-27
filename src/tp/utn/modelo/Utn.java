package tp.utn.modelo;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import tp.utn.ann.Column;
import tp.utn.ann.Id;
import tp.utn.ann.Table;
import tp.utn.demo.domain.Persona;

public class Utn
{
	//el codigo en la funcion armar select y armar from es MUY parecido, ver como se podria hacer los dos en uno solo
	// Retorna: el SQL correspondiente a la clase dtoClass acotado por xql
	public static <T> String _query(Class<T> dtoClass, String xql)
	{
		String consulta = "";
		
		//Validaciones
		//Que exista la annotation @Table
		Annotation esTabla= dtoClass.getAnnotation(Table.class);
		try{
			if (esTabla == null && !(esTabla instanceof Table)){
				throw new Exception("Clase no mapeada");
			}
		}
		catch (Exception e){
			return e.getMessage(); //Agregar mejores excepciones
		}
		//Que tenga un id
		//Armar el array con todos los fields
		Field[] atributosClase = dtoClass.getDeclaredFields();
		List<Field> listaAtributosClase = new ArrayList<Field>();
		for(Field f:atributosClase){
			listaAtributosClase.add(f);
		}
		
		//Busco el id
		try{
			if (buscarIdTabla(dtoClass) == null){
				throw new Exception("Clase sin id");
			}
		}
		catch (Exception e){
			return e.getMessage(); //Agregar mejores excepciones
		}
		
		//Las tres funciones se pueden extaer a objetos tranquilamente, pero no veo la necesidad aún
		consulta = armarSelect(dtoClass) + armarFrom(dtoClass) + armarWhere(dtoClass, xql);
		return consulta;//query;
	}
	
	private static <T> String armarSelect(Class<T> dtoClass){
		String select = Ormsql.select()+Ormsql.salto();
		List<String> listaSelect = new ArrayList<>();
		
		String aliasTabla; 
		//en el select me es indistinto validar en dos variable si usar nombre o alias, asumo que en el from si hay alias lo declaro
		Annotation esTabla= dtoClass.getAnnotation(Table.class);
		if ((((Table)esTabla).alias()).length() > 0){aliasTabla = ((Table)esTabla).alias();}else{aliasTabla = ((Table)esTabla).name();}
		
		//Hago una funcion que agarre un array y lo llene, por cada atributo de tipo @column final (final = noOtroObjetoDeDominio),
		// con aliasTabla + Ormsql.punto() + nombreCampo (sale de la anottation)		
		listaSelect = buscarAtributos(dtoClass, aliasTabla, listaSelect);
		//al final recorro el array y concateno en un string itemArray + Ormsql.coma() + Ormsql.salto() + Ormsql.tab() + otroItem...			
		select += Ormsql.tab()+listaSelect.get(0);
		listaSelect.remove(0);
		select += listaSelect.stream().reduce("", (a, b) -> a + Ormsql.coma() + Ormsql.salto() +  Ormsql.tab() + b);
		select += Ormsql.salto();
		return select;
	}
	
	private static <T> List<String> buscarAtributos(Class<T> dtoClass, String aliasTabla, List<String> listaAtributos){
		//Agarrar cada field y ver sus annotations
		Field[] atributosClase = dtoClass.getDeclaredFields();
		for(Field atributo:atributosClase){
			Annotation[] anotacionesAtributo = atributo.getDeclaredAnnotations();
			for(Annotation  anotacion:anotacionesAtributo){
				//Si es @column	
				if (anotacion instanceof Column){
					if (((Column)anotacion).fetchType() == Column.EAGER){ //Si es Eager lo busco, si es Lazy no
						Class<T> otraTabla = (Class<T>)atributo.getType();
						Annotation anotacionTablaOtraTabla = otraTabla.getAnnotation(Table.class);
						if(anotacionTablaOtraTabla == null) //no es otro objeto de dominio
							{listaAtributos.add(aliasTabla + Ormsql.punto() + ((Column)anotacion).name());}
						else{ 													//es otro objeto de dominio
							//Llamo recursivamente a la funcion con la clase del objeto de dominio
							String aliasOtraTabla; 
							//en el select me es indistinto validar en dos variable si usar nombre o alias, asumo que en el from si hay alias lo declaro
							if ((((Table)anotacionTablaOtraTabla).alias()).length()>0){aliasOtraTabla = ((Table)anotacionTablaOtraTabla).alias();}
							else{aliasOtraTabla = ((Table)anotacionTablaOtraTabla).name();}
							buscarAtributos(otraTabla, aliasOtraTabla, listaAtributos);
						}	
					}
				}
				else{//si es @Relation
					
				}
			}//Fin recorrido anotaciones
		}//Fin recorrido atributos
		return listaAtributos;
	}
	
	private static <T> String armarFrom(Class<T> dtoClass){
		String from = Ormsql.from()+Ormsql.salto();
		List<String> listaJoin = new ArrayList<>();
		
		//el from es from + salto + tab mas nombreTabla + espacio + as + espacio + alias + left join + nombreTabla alias on alias = 
		//uso hashmap nombre tabla + nombre campo que las une (id_...) 
		//@column on p.idDireccion
		//necesito nombre tabla, alias, campo idTabla2 tabla1 y campo id tabla 2
		String aliasTabla, nombreId;
		Annotation esTabla= dtoClass.getAnnotation(Table.class);
		if ((((Table)esTabla).alias()).length() > 0){
			aliasTabla = ((Table)esTabla).alias();
			from += Ormsql.tab()+((Table)esTabla).name()
					+Ormsql.espacio()+Ormsql.as()+Ormsql.espacio()+
					((Table)esTabla).alias()+Ormsql.salto();
		}
		else{
			aliasTabla = ((Table)esTabla).name();
			from += Ormsql.tab()+((Table)esTabla).name()+Ormsql.salto();
		}
				
		listaJoin = buscarJoin(dtoClass, aliasTabla, listaJoin);		
		
		from += listaJoin.stream().reduce("", (a, b) -> a +  Ormsql.tab() + b + Ormsql.salto());
		return from;
	}
	
	private static <T> List<String> buscarJoin(Class<T> dtoClass, String aliasTabla, List<String> listaJoin){
		Field[] atributosClase = dtoClass.getDeclaredFields();
		for(Field atributo:atributosClase){
			Annotation[] anotacionesAtributo = atributo.getDeclaredAnnotations();
			for(Annotation  anotacion:anotacionesAtributo){
				//Si es @column	
				if (anotacion instanceof Column){
					if (((Column)anotacion).fetchType() == Column.EAGER){ //Si es Eager lo busco, si es Lazy no
						Class<T> otraTabla = (Class<T>)atributo.getType();
						Annotation anotacionTablaOtraTabla = otraTabla.getAnnotation(Table.class);
						if(anotacionTablaOtraTabla != null){ 
							//es otro objeto de dominio
							String nombreOtraTabla;
							String aliasOtraTabla; 
							String idOtraTabla = buscarIdTabla(otraTabla);
							if ((((Table)anotacionTablaOtraTabla).alias()).length()>0){
								aliasOtraTabla = ((Table)anotacionTablaOtraTabla).alias();
								nombreOtraTabla = ((Table)anotacionTablaOtraTabla).name()
													+Ormsql.espacio()+Ormsql.as()+Ormsql.espacio()+
													aliasOtraTabla;
							}
							else{
								nombreOtraTabla = ((Table)anotacionTablaOtraTabla).name();
								aliasOtraTabla =nombreOtraTabla;
							}
							//agrego el aliasTabla.column.name + aliasOtraTabla.el nombre del id del otro objeto al hashmap
							listaJoin.add(
									Ormsql.left()+Ormsql.espacio()+Ormsql.join()+Ormsql.espacio()+
									nombreOtraTabla+Ormsql.espacio()+
									Ormsql.on()+Ormsql.espacio()+
									Ormsql.espacio()+aliasTabla+Ormsql.punto()+((Column)anotacion).name()+//p.id_direccion
									Ormsql.espacio()+Ormsql.igual()+Ormsql.espacio()+
									aliasOtraTabla+Ormsql.punto()+idOtraTabla
									//left join direccion as d on p.id_direccion = d.id_direccion 	
									);
							buscarJoin(otraTabla, aliasOtraTabla, listaJoin);
						}
					}
				}
				else{//si es @Relation
						
				}
			}//Fin recorrido anotaciones
		}//Fin recorrido atributos
		
		return listaJoin;
	}
	
	private static <T> String buscarIdTabla(Class<T> dtoClass){
		String id = "";
		Field[] atributosClase = dtoClass.getDeclaredFields();
		for(Field atributo:atributosClase){
			Annotation[] anotacionesAtributo = atributo.getDeclaredAnnotations();
			if(atributo.isAnnotationPresent(Id.class)){
				Annotation  anotacionColumnaId = atributo.getAnnotation(Column.class);
				return ((Column)anotacionColumnaId).name();
			}
		}//Fin recorrido atributos
		return null; //tengo que ver de hacer un metodo que valide si tiene id, si tiene el @table, etc para usarlo en _query y aca
	}
	
	private static <T> String armarWhere(Class<T> dtoClass, String xql){
		if(xql == "" || xql == null){
			return "";
		}	
		return Ormsql.salto()+Ormsql.where()+Ormsql.salto()+Ormsql.tab()+xql;
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	
	
	// Invoca a: _query para obtener el SQL que se debe ejecutar
	// Retorna: una lista de objetos de tipo T
	public static <T> List<T> query(Connection con, Class<T> dtoClass, String xql, Object... args)
	{
		return null;
	}
	
	// Retorna: una fila identificada por id o null si no existe
	// Invoca a: query
	private static <T> T find(Connection con, Class<T> dtoClass, Object id)
	{
		return null;
	}
	
	// Retorna: una todasa las filas de la tabla representada por dtoClass
	// Invoca a: query
	private static <T> List<T> findAll(Connection con, Class<T> dtoClass)
	{
		return null;
	}

	// Retorna: el SQL correspondiente a la clase dtoClass acotado por xql
	public static <T> String _update(Class<T> dtoClass, String xql)
	{
		return null;
	}
	
	// Invoca a: _update para obtener el SQL que se debe ejecutar
	// Retorna: la cantidad de filas afectadas luego de ejecutar el SQL
	public static int update(Connection con, Class<?> dtoClass, String xql, Object... args)
	{
		return 0;
	}

	// Invoca a: update 
	// Que hace?: actualiza todos los campos de la fila identificada por el id de dto
	// Retorna: Cuantas filas resultaron modificadas (deberia: ser 1 o 0) 
	public static int update(Connection con, Object dto)
	{
		return 0;
	}
	
	// Retorna: el SQL correspondiente a la clase dtoClass acotado por xql
	public static String _delete(Class<?> dtoClass, String xql)
	{
		return null;
	}
	
	// Invoca a: _delete para obtener el SQL que se debe ejecutar
	// Retorna: la cantidad de filas afectadas luego de ejecutar el SQL
	public static int delete(Connection con, Class<?> dtoClass, String xql, Object... args)
	{
		return 0;
	}

	// Retorna la cantidad de filas afectadas al eliminar la fila identificada por id
    // (deberia ser: 1 o 0)
	// Invoca a: delete
	public static int delete(Connection con, Class<?> dtoClass, Object id)
	{
		return 0;
	}

	// Retorna: el SQL correspondiente a la clase dtoClass
	public static String _insert(Class<?> dtoClass)
	{
		return null;
	}
	
	// Invoca a: _insert para obtener el SQL que se debe ejecutar
	// Retorna: la cantidad de filas afectadas luego de ejecutar el SQL
	public static int insert(Connection con, Object dto)
	{
		return 0;
	}
}
