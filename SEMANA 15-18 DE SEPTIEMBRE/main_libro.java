package sesion1.encapsulamiento;

/**
 *
 * @author Jose Leguizamon
 */
public class Sesion1Encapsulamiento {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
      Libro libro1 = new Libro ();
      libro1.setTitulo("el quijote de la mancha");
      libro1.setAutor("Miguel de cervantes saavedra");
      libro1.setGenero("novela");
      libro1.setNumPaginas(600);
      
      System.out.println(libro1.getTitulo());
      System.out.println(libro1.getAutor());
      System.out.println(libro1.getGenero());
      System.out.println(libro1.getNumPaginas());
      
      System.out.println("\nDETALLE DEL LIBRO");
      System.out.println(libro1.verDetalle());
      
      System.out.println("\nLIBRO PRESTADO A");
      System.out.println(libro1.prestar("JOSE"));
      
      System.out.println("\nLIBRO PRESTADO Y DEVUELTO");
      System.out.println(libro1.prestarYDevolver("Jose"));
    }
}
