package principal;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.neodatis.odb.ODB;
import org.neodatis.odb.ODBFactory;
import org.neodatis.odb.ObjectValues;
import org.neodatis.odb.Objects;
import org.neodatis.odb.Values;
import org.neodatis.odb.core.query.criteria.Where;
import org.neodatis.odb.core.query.nq.SimpleNativeQuery;
import org.neodatis.odb.impl.core.query.criteria.CriteriaQuery;
import org.neodatis.odb.impl.core.query.values.ValuesCriteriaQuery;
import org.neodatis.odb.core.query.IQuery;

import datos.Estudiantes;
import datos.Participa;
import datos.Proyectos;

public class Principal {
	private static ODB bd;
	private static Connection conexion;
    public static Scanner teclado = new Scanner(System.in);

    public static void main(String[] args) {
        try {
            Class.forName("oracle.jdbc.driver.OracleDriver");
            Connection conexion = DriverManager.getConnection("jdbc:oracle:thin:@localhost:1521:XE", "PROYECTOS", "proyectos");

            
            int opcion = 1;
            do {
                menu();
                System.out.println("Elige una opción");
                opcion = teclado.nextInt();
                switch (opcion) {
                    case 1:
                        crearBD(conexion);
                        break;
                    case 2:
                        listarProyecto(1);
                        break;
                    case 3:
                        insertarParticipacion(1,1,"prueba",3,conexion);
                        break;
                    case 0:
                        System.out.println("Saliendo...");
                        break;
                    default:
                        System.out.println("Opción no válida. Inténtalo de nuevo.");
                }

            } while (opcion != 0);

            conexion.close();
            //bd.close();
            teclado.close();
        } catch (ClassNotFoundException cn) {
            cn.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void insertarParticipacion(int codEstudiante, int codProyecto, String tipoAportacion, int numAportacion, Connection conexion) {
        bd = ODBFactory.open("proyectos.dat");
        boolean error = false;

        try {
            if (!comprobarEstudiante(codEstudiante)) {
                System.out.println("Error código: " + codEstudiante);
                error = true;
            }

            if (!comprobarProyecto(codProyecto)) {
                System.out.println("Error: " + codProyecto);
                error = true;
            }

            if (!error) {
                Values values = bd.getValues(new ValuesCriteriaQuery(Participa.class).max("codparticipacion", "codmax"));
                ObjectValues o = values.nextValues();
                BigDecimal max = (BigDecimal) o.getByAlias("codmax");
                Estudiantes estudiante = obtenerEstudiantePorCodigo(codEstudiante);
                Proyectos proyecto = obtenerProyectoPorCodigo(codProyecto);
                int codigoparti = Integer.parseInt(max.toString()) + 1;

                if (estudiante != null && proyecto != null) {
                    Participa parti = new Participa(codigoparti, estudiante, proyecto, tipoAportacion, numAportacion);
                    bd.store(parti);
                    bd.commit();
                    System.out.println("Correcto: " + codigoparti);
                    rellenarProyectos(conexion);
                    rellenarEstudiantes(conexion);
                } else {
                    System.out.println("Error");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            error = true;
        
            bd.close(); 
        }
    

    }



    


	private static void listarProyecto(int cod) {
    	bd = ODBFactory.open("proyectos.dat");// Abrir BD
        try {
            // Código del proyecto de prueba
        	//int codigoProyecto = 100;
            int codigoProyecto = 1;

            Proyectos proyecto = obtenerProyectoPorCodigo(codigoProyecto);

            if (proyecto != null) {
                System.out.println("--------------------- ------------- -----------------------------");
                System.out.println("Código proyecto: " + proyecto.getCodigoproyecto()+"           Nombre: " + proyecto.getNombre());
                System.out.println("Fecha inicio: " + proyecto.getFechainicio()+"     Fecha fin: " + proyecto.getFechafin());
                System.out.println("Presupuesto: " + proyecto.getPresupuesto()+"        Extraaportación: " + proyecto.getExtraaportacion());
                System.out.println("--------------------- --------------------- -----------------------");

                System.out.println("Participantes en el proyecto:");
                System.out.println("----------------------------");

                List<Participa> participantes = proyecto.getParticipantes();

                if (!participantes.isEmpty()) {
                    System.out.printf("%-16s %-13s %-22s %-13s %-15s %-7s%n", "CODPARTICIPACION", "CODESTUDIANTE", "NOMBREESTUDIANTE", "TIPAPORTACION", "NUMAPORTACIONES ", "IMPORTE");
                    System.out.printf("%16s %13s %22s %13s %15s %7s%n", "----------------", "-------------","----------------------", "-------------", "---------------", " -------");

                    int totalAportaciones = 0;
                    float totalImporte = 0;

                    for (Participa participa : participantes) {
                        int numAportaciones = participa.getNumaportaciones();
                        float importe = numAportaciones * proyecto.getExtraaportacion();
                        totalAportaciones += numAportaciones;
                        totalImporte += importe;

                        System.out.printf("%-16s %-13s %-22s %-13s %-16s %-8s%n",
                                participa.getCodparticipacion(),
                                participa.getEstudiante().getCodestudiante(),
                                participa.getEstudiante().getNombre(),
                                participa.getTipoparticipacion(),
                                numAportaciones,
                                importe);
                    }

                    System.out.printf("%16s %13s %22s %13s %15s %7s%n", "----------------", "-------------","----------------------", "-------------", "---------------", " -------");
                    System.out.println("TOTALES:							    " + totalAportaciones + "                " + totalImporte);
                } else {
                    System.out.println("No hay participantes en el proyecto.");
                }
            } else {
                System.out.println("El proyecto no existe en la BD.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        bd.close(); // Cerrar BD
    }

	private static void menu() {

        System.out.println();
        System.out.println("1. Crear BD");
        System.out.println("2. Listar un proyecto");
        System.out.println("3. INSERTAR PARTICIPACIÓN");
        System.out.println("0. Salir");
    }

    private static void crearBD(Connection conexion) throws SQLException {
    	bd = ODBFactory.open("proyectos.dat");
        rellenarProyectos(conexion);
        rellenarEstudiantes(conexion);
        rellenarParticipa(conexion);
        rellenarListaParticipaProyecto(conexion);
        rellenarListaEstudiantes(conexion);
        bd.close(); // Cerrar BD
    }

    private static void rellenarParticipa(Connection conexion) {//
        try {
            Statement sentencia = conexion.createStatement();
            ResultSet resulParticipa = sentencia.executeQuery("SELECT * FROM PARTICIPA");

            while (resulParticipa.next()) {
            	
                int codParticipa = resulParticipa.getInt(1);
                
                if (!comprobarParticipa(codParticipa)) {
                	 int codEstudiante = resulParticipa.getInt(2);
                     int codProyecto = resulParticipa.getInt(3);
                     Estudiantes estudiante = obtenerEstudiantePorCodigo(codEstudiante);
                     Proyectos proyecto = obtenerProyectoPorCodigo(codProyecto);
                     
                     if (estudiante != null && proyecto != null) {
                    	 Participa participa = new Participa();
                         participa.setCodparticipacion(codParticipa);
                         participa.setEstudiante(estudiante);
                         participa.setProyecto(proyecto);
                         participa.setTipoparticipacion(resulParticipa.getString(4));
                         participa.setNumaportaciones(resulParticipa.getInt(5));
                         bd.store(participa);
 						System.out.println("Correcto: " + codParticipa);
                     }else {
                    	 System.out.println("No hay "+codEstudiante + codProyecto);
                     }
                    
                   
                } else {
                    System.out.println("Participa: " + codParticipa + ", EXISTE.");
                }
            }
            bd.commit();
            resulParticipa.close();
            sentencia.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static boolean comprobarParticipa(int codParticipacion) {//

    	try {
            IQuery consultarParticipa = new CriteriaQuery(Participa.class, Where.equal("codparticipacion", codParticipacion));
            Object participaciones = bd.getObjects(consultarParticipa).getFirst();
            return participaciones!= null;
        } catch (Exception e) {
            return false;
        }
    }
    private static boolean comprobarEstudiante(int codEstudiante) {//
    	 try {
    	        IQuery consultarEstudiante = new CriteriaQuery(Estudiantes.class, Where.equal("codestudiante", codEstudiante));
    	        Objects<Object> estudiantes = bd.getObjects(consultarEstudiante);
    	        return estudiantes.size() > 0;  
    	    } catch (Exception e) {
    	        return false;
    	    }
    }
    private static boolean comprobarProyecto(int codProyecto) {//

    	try {
            IQuery consultarProyecto = new CriteriaQuery(Proyectos.class, Where.equal("codigoproyecto", codProyecto));
            Objects<Object> proyectos = bd.getObjects(consultarProyecto);
            return proyectos.size() > 0;
        } catch (Exception e) {
            return false;
        }
    }
    
    
    private static void rellenarEstudiantes(Connection conexion) {//
        try {
            Statement sentencia = conexion.createStatement();
            ResultSet resulEstudiantes = sentencia.executeQuery("SELECT * FROM ESTUDIANTES");

            while (resulEstudiantes.next()) {
                if (comprobarEstudiante(resulEstudiantes.getInt(1))==false) {
                	List<Participa> listaparticipa = new ArrayList<Participa>();
                    Estudiantes estudiante = new Estudiantes();
                    estudiante.setCodestudiante(resulEstudiantes.getInt(1));
                    estudiante.setNombre(resulEstudiantes.getString(2));
                    estudiante.setDireccion(resulEstudiantes.getString(3));
                    estudiante.setTlf(resulEstudiantes.getString(4));
                    estudiante.setFechaalta(resulEstudiantes.getDate(5));

                    bd.store(estudiante);
                    System.out.println("Correctamente "+ resulEstudiantes.getString(1));
                } else {
                    System.out.println("Estudiante: " + resulEstudiantes.getString(1) + ", EXISTE.");
                }
            }
            bd.commit();
            resulEstudiantes.close();
            sentencia.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

   
    private static void rellenarListaEstudiantes(Connection conexion) throws SQLException {//
        //ArrayList<Participa> listaParticipa = new ArrayList<>();
        
        Objects<Estudiantes> objects = bd.getObjects(Estudiantes.class);

		while (objects.hasNext()) {

			Estudiantes es = objects.next();

			List<Participa> listaparticipa = new ArrayList<Participa>();

			Statement sentencia = conexion.createStatement();

			ResultSet resul = sentencia.executeQuery("SELECT * FROM participa where codestudiante = " + es.getCodestudiante());

			while (resul.next()) {
				IQuery consulta = new CriteriaQuery(Participa.class, Where.equal("codparticipacion", resul.getInt(1)));

				Objects<Participa> partObjects = bd.getObjects(consulta);

				// Verificar si hay resultados antes de intentar obtener el primero
				if (partObjects.size() > 0) {
					Participa obj = partObjects.getFirst();
					listaparticipa.add(obj);
				}

			}

			es.setParticipaen(listaparticipa);
			;

			bd.store(es);

			resul.close();

			sentencia.close();

		}

		bd.commit();
		
    }

   

    private static void rellenarProyectos(Connection conexion) {//
        try {
            Statement sentencia = conexion.createStatement();
            ResultSet resulProyectos = sentencia.executeQuery("SELECT * FROM PROYECTOS");

            while (resulProyectos.next()) {
               
                if (comprobarProyecto(resulProyectos.getInt(1)) == false) {
                	List<Participa> listaparticipa = new ArrayList<Participa>();
                    Proyectos proyecto = new Proyectos();
                    proyecto.setCodigoproyecto(resulProyectos.getInt(1));
                    proyecto.setNombre(resulProyectos.getString(2));
                    proyecto.setFechainicio(resulProyectos.getDate(3));
                    proyecto.setFechafin(resulProyectos.getDate(4));
                    proyecto.setPresupuesto(resulProyectos.getFloat(5));
                    proyecto.setExtraaportacion(resulProyectos.getFloat(6));


                    bd.store(proyecto);
                    System.out.println("Correctamente: "+resulProyectos.getString(1));
                } else {
                    System.out.println("Proyecto: " + resulProyectos.getString(1) + ", EXISTE.");
                }
            }
            bd.commit();
            resulProyectos.close();
            sentencia.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

   

    private static void rellenarListaParticipaProyecto(Connection conexion) throws SQLException {//
        
        Objects<Proyectos> objects = bd.getObjects(Proyectos.class);

		while (objects.hasNext()) {

			Proyectos pro = objects.next();

			List<Participa> listaparticipa = new ArrayList<Participa>();

			Statement sentencia = conexion.createStatement();

			ResultSet resul = sentencia.executeQuery("SELECT * FROM participa where codigoproyecto = " + pro.getCodigoproyecto());

			while (resul.next()) {
				IQuery consulta = new CriteriaQuery(Participa.class, Where.equal("codparticipacion", resul.getInt(1)));

				Objects<Participa> partObjects = bd.getObjects(consulta);

				// Verificar si hay resultados antes de intentar obtener el primero
				if (partObjects.size() > 0) {
					Participa obj = partObjects.getFirst();
					listaparticipa.add(obj);
				}

			}

			pro.setParticipantes(listaparticipa);
			;

			bd.store(pro);

			resul.close();

			sentencia.close();

		}

		bd.commit();
		
    }

    private static Estudiantes obtenerEstudiantePorCodigo(int codEstudiante) {//
		try {
			IQuery consulta = new CriteriaQuery(Estudiantes.class, Where.equal("codestudiante", codEstudiante));
			return (Estudiantes) bd.getObjects(consulta).getFirst();
		} catch (IndexOutOfBoundsException e) {
			return null;
		}
	}

	private static Proyectos obtenerProyectoPorCodigo(int codProyecto) {//
		try {
			IQuery consulta = new CriteriaQuery(Proyectos.class, Where.equal("codigoproyecto", codProyecto));
			return (Proyectos) bd.getObjects(consulta).getFirst();
		} catch (IndexOutOfBoundsException e) {
			return null;
		}
	}
}