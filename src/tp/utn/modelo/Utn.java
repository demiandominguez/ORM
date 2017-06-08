package tp.utn.modelo;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import tp.utn.ConnectDatabase;
import tp.utn.ann.Column;
import tp.utn.ann.Id;
import tp.utn.ann.Table;

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
							{listaAtributos.add(aliasTabla + Ormsql.punto() + ((Column)anotacion).name() + Ormsql.espacio() + Ormsql.as() + Ormsql.espacio()
									+ aliasTabla + Ormsql.underscore() + ((Column)anotacion).name() );}
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
		from = from.substring(0,from.length()-1);
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
							String idOtraTabla = (buscarIdTabla(otraTabla).getAnnotation(Column.class)).name();
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
			}//Fin recorrido anotaciones
		}//Fin recorrido atributos
		
		return listaJoin;
	}
	
	private static <T> Field buscarIdTabla(Class<T> dtoClass){
		String id = "";
		Field[] atributosClase = dtoClass.getDeclaredFields();
		for(Field atributo:atributosClase){
			Annotation[] anotacionesAtributo = atributo.getDeclaredAnnotations();
			if(atributo.isAnnotationPresent(Id.class)){
				return atributo;
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
	
	// Invoca a: _query para obtener el SQL que se debe ejecutar
	// Retorna: una lista de objetos de tipo T
	public static <T> List<T> query(Connection con, Class<T> dtoClass, String xql, Object... args) throws Exception
	{
		String xql_mapped;
		try
		{
			xql_mapped=mapXql(dtoClass, xql,args);
		}
		catch(Exception e)
		{
			throw new Exception(e.getMessage());
		}
		String consulta = _query(dtoClass,xql_mapped);
		PreparedStatement pstmt = null;
		ResultSet result = null;
		List<T> listaResultados = new ArrayList<T>();
	    try {
	    	pstmt = con.prepareStatement(consulta);
	    	result = pstmt.executeQuery();
	    	result.next();
	    	return (List<T>)populate(result, dtoClass);
	    } catch (Exception e) {
	    	throw e;
	    }	
	    finally {
	    	if( result!=null ) result.close(); 
	    	if( pstmt!=null ) pstmt.close();
		}
	}
	
	private static <T> Collection<T> populate(ResultSet result, Class<T> dtoClass) throws Exception
	{
		List<T> resultSetHidratado = new ArrayList<>();
		try
		{
			do{
				resultSetHidratado.add((T)hidrate(result,dtoClass));
			}while (result.next());
			return resultSetHidratado;
		}
		catch(SQLException e)
		{
			throw e;
		}
	}
	
	public static <T> T hidrate (ResultSet result, Class<T> dtoClass) throws Exception
	{
		Enhancer enhancer = new Enhancer();
		enhancer.setSuperclass(dtoClass);
		enhancer.setCallback(new MethodInterceptor() {
			@Override
			public Object intercept(Object obj, Method metodo, Object[] args, MethodProxy proxy) throws Throwable 
			{
				Type tipo = metodo.getReturnType();
				if(tipo!=null){
					Field[] atributos = metodo.getDeclaringClass().getDeclaredFields();
					for(Field atributo:atributos){
						if(atributo.getType().equals(tipo)){
							if(atributo.isAnnotationPresent(Column.class))
							{
								Annotation anotacion = atributo.getAnnotation(Column.class);
								if(((Column)anotacion).fetchType() == Column.LAZY && atributo != null){
									ConnectDatabase conector = new ConnectDatabase();
									Connection con = conector.getConnection();
									String xql = "";
									xql = Ormsql.$() + (((Class<T>)tipo).getAnnotation(Table.class)).name() + Ormsql.punto() + Utn.buscarIdTabla((Class<T>)tipo).getName()
											+ Ormsql.igual() + Ormsql.incognita();
									Field atrGet = Utn.buscarIdTabla((Class<T>)metodo.getDeclaringClass());
									//falta sacar el atributo del obj
									Method metodoGet = ((Class<T>)metodo.getDeclaringClass()).getDeclaredMethod("get" + atrGet.getName().substring(0,1).toUpperCase() + atrGet.getName().substring(1));
									return Utn.query(con,(Class<T>)tipo,xql,metodoGet.invoke(obj, null)).get(0);
								}
							}
							else{
								if(atributo.isAnnotationPresent(tp.utn.ann.Relation.class)){
									System.out.println("es relation");
									Annotation anotacion = atributo.getAnnotation(tp.utn.ann.Relation.class);
									
								}
							}
						}
				    }	
				}
				return proxy.invokeSuper(obj, args);
			}
		});
		T newT = (T) enhancer.create();
		//estoy considerando siempre que los metodos get y set siguen la form set/getCampo por que no se como se podria hacer de otra forma.
		//capaz se puede buscar el metodo que tenga returnType del tipo del atributo que quiero settear pero si hay otro etodo que tenga el mismo tipo me traeria el primero
		//o podria no tener un tipo de retorno
		String nombreTabla = getNombreOAlias(dtoClass);
		Field[] atributosClase = dtoClass.getDeclaredFields();
		for(Field atributo:atributosClase){
			try{
				Annotation anotacion = atributo.getAnnotation(Column.class);
				if (anotacion != null &&((Column)anotacion).fetchType() == Column.EAGER){ //Si es Eager lo busco, si es Lazy no
					Method metodo = ((Class<T>)dtoClass).getDeclaredMethod("set" + atributo.getName().substring(0,1).toUpperCase() + atributo.getName().substring(1),atributo.getType());
					Class<T> otraTabla = (Class<T>)atributo.getType();
					Annotation anotacionTablaOtraTabla = otraTabla.getAnnotation(Table.class);
					if(anotacionTablaOtraTabla != null){
						//si es un campo compuesto
						metodo.invoke(newT, hidrate(result,otraTabla));
						//hay un problema al sacar los valores del result set: buscar forma de sacar los valores usando el nombre del campo que lo saco de la anotation
					}
					else{
						//si es un campo simple ejemplo persona.id, persona.nombre
						metodo.invoke(newT,result.getObject( ( nombreTabla + Ormsql.underscore() + (atributo.getAnnotation(Column.class)).name().toUpperCase() ), atributo.getType() ));
					}
				}
			}
			catch (NoSuchMethodException | SecurityException e) {
	            e.printStackTrace();
	        } catch (IllegalArgumentException e) {
	            e.printStackTrace();
	        }
		}//Fin recorrido atributos
		return newT;
	}

	public static <T> String mapXql(Class<T> dtoClass, String xql, Object... args) throws Exception
	{
		int contadorIncognitas = 0;
		//busco el simbolo pesos en el xql, cuando lo encuentro me guardo el index 
		int pos$ = xql.indexOf(Ormsql.$());
		if (pos$ == -1){
			return xql;
		}
		
		do{
		String nombreAtributo = "";
		int finAtributo = getPosFinAtributo(pos$, xql); //busco el proximo caracter especial
		if((pos$ + 1 == finAtributo)|| (finAtributo == -1))
		{
			throw new Exception("XQL mal armado");	//puso un $ sin poner que variable hay que reempazar o puso $.algo
		}
		if (xql.substring(pos$ + 1,finAtributo).compareToIgnoreCase(dtoClass.getSimpleName()) == 0){
			//aca es $nombreclase.nombreatributo
			pos$ = finAtributo + 1;
			finAtributo =  getPosFinAtributo(pos$, xql);
			if((finAtributo == -1)||(finAtributo == (pos$ + 1))){
				throw new Exception("XQL mal armado");
			}
			try
			{
				xql = reemplazarNombreAributo(dtoClass, pos$, finAtributo, xql);
			}
			catch(Exception e)
			{
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}
		else{
			//aca es nombreatributo
			finAtributo =  getPosFinAtributo(pos$, xql);
			if((finAtributo == -1)||(finAtributo == (pos$ + 1))){
				throw new Exception("XQL mal armado");
			}
			try
			{
				xql = reemplazarNombreAributo(dtoClass, pos$ + 1, finAtributo, xql);
			}
			catch(Exception e)
			{
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}
		//siempre que encuentro un atributo me fijo si hay un ?, si hay reemplazo por el valor de args correspondiente
		
		if(xql.indexOf(Ormsql.incognita()) != -1 ){
			if(args.length > contadorIncognitas){
				int posIncognita = xql.indexOf(Ormsql.incognita());
				xql = xql.substring(0,posIncognita) + args[contadorIncognitas] + xql.substring(posIncognita + 1);
				contadorIncognitas++;
			}
			else{
				throw new Exception("Cantidad de parámetros insuficiente");
			}
		}
		pos$ = xql.indexOf(Ormsql.$());
		} while (pos$ != -1);
		return xql;
	}


	
	private static <T> String getNombreOAlias(Class<T> dtoClass)
	{
		Annotation esTabla= dtoClass.getAnnotation(Table.class);
		try{
			if (esTabla == null && !(esTabla instanceof Table)){
				throw new Exception("Clase no mapeada");
			}
		}
		catch (Exception e){
			return e.getMessage(); //Agregar mejores excepciones
		}
		if ((((Table)esTabla).alias()).length() > 0){return ((Table)esTabla).alias();}else{return ((Table)esTabla).name();}
	}

	private static int getPosFinAtributo(int pos$, String xql)
	{
		//buscar la posicion de fin del atributo donde este el primer espacio < > = o espacio_blanco
		List<Integer> posiciones = new ArrayList<>();
		if(xql.indexOf(".", pos$) != -1 ) posiciones.add(xql.indexOf(".", pos$));
		if(xql.indexOf("=", pos$) != -1 ) posiciones.add(xql.indexOf("=", pos$));
		if(xql.indexOf("<", pos$) != -1 ) posiciones.add(xql.indexOf("<", pos$));
		if(xql.indexOf(">", pos$) != -1 ) posiciones.add(xql.indexOf(">", pos$));
		if(xql.indexOf(" ", pos$) != -1 ) posiciones.add(xql.indexOf(" ", pos$));
		if(posiciones.isEmpty()){return -1;}
		else {return posiciones.stream().mapToInt(i -> i).min().getAsInt();}
	}

	private static <T> String reemplazarNombreAributo(Class<T> dtoClass, int pos$, int finAtributo, String xql) throws Exception
	{
		String substring =  xql.substring(pos$, finAtributo);
		Field[] atributosClase = dtoClass.getDeclaredFields();
		for(Field atributo:atributosClase){
			if(atributo.getName().equals(substring))
			{
				if(atributo.isAnnotationPresent(Column.class)){
					Class<T> otraTabla = (Class<T>)atributo.getType();
					Annotation anotacionTablaOtraTabla = otraTabla.getAnnotation(Table.class);
					if(anotacionTablaOtraTabla != null){ 
						//es persona.direccion.id o direccion.id
						return reemplazarNombreAributo(otraTabla, finAtributo + 1, getPosFinAtributo(finAtributo + 1,xql), xql);
					}
					else{
						//es atributo de persona
						Annotation[] anotacionesAtributo = atributo.getDeclaredAnnotations();
						Annotation  anotacionColumna = atributo.getAnnotation(Column.class);
						return xql.substring(0,xql.indexOf(Ormsql.$())) + getNombreOAlias(dtoClass) + Ormsql.punto() + ((Column)anotacionColumna).name() + xql.substring(finAtributo); 
						//esto no anda xql.replace(cadenaAReemplazar, nombreAtributo); String cadenaAReemplazar = xql.substring(xql.indexOf(Ormsql.$()),finAtributo);
					}
				}
			}
		}//Fin recorrido atributos
		throw new Exception("Atributo No Existente");
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
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
