import java.awt.print.PrinterException;
import java.io.*;
import java.net.*;
import java.util.*;
import java.text.SimpleDateFormat;
import java.lang.Thread;

import autenticacion.*;


class NuevoHilo extends Thread {
	Thread t;
	Autenticar_Usuario usuario;
	static String mensaje;
	String log;
	String clave;
	static String existe;
	//static String entrada;
	static String[] palabra;
	bitacora reg = new bitacora();
	static directorio_server listar = new directorio_server();
	private Socket cliente;
	static String nombre_archivo_subir;
	static String nombre_archivo_bajar;
	static int archivo_len;
	InputStream flujoentrada;
	BufferedReader entrada;
	OutputStream flujosalida;
	PrintStream salida;
	manejo_flujos flujos;
	boolean finalizar = false;
	
	NuevoHilo(Socket socket_cliente) {
		this.cliente = socket_cliente;
		Thread t;
		t = new Thread(this);
		System.out.println("Cliente: " + t);
		System.out.println("Accepted connection : " + socket_cliente);
		try{
			this.flujoentrada = cliente.getInputStream();
			this.entrada = new BufferedReader(new InputStreamReader(this.flujoentrada));
			this.flujosalida = cliente.getOutputStream();
			this.salida = new PrintStream (this.flujosalida);
			this.flujos = new manejo_flujos();
		} catch (Exception e) {e.printStackTrace();}
		t.start();
	}
	
	public void run() {
		try{			
			do{
				flujos.enviar_mensaje("conexion aceptada", salida);
				mensaje = entrada.readLine();
				palabra = mensaje.split(" ");
				this.log = palabra[1];
				this.clave = palabra[2];
				usuario = new Autenticar_Usuario(this.log, this.clave);
				existe = this.usuario.Autenticar(this.log, this.clave);
				if(palabra[0].equals("1")){
					if(existe.equals("si")){           // EL USUARIO SI EXISTE
						this.reg.crear_bitacora(this.log);
						this.reg.escribir_bitacora(this.log, "autenticado");
						flujos.enviar_mensaje("Usuario Autenticado. Hola " + this.log, salida);
						while(!finalizar){
							System.out.println("esperando comando");
							mensaje = entrada.readLine();
							while(true){
							  if(mensaje.equals("ds")){
								  System.out.println("listando directorio");
								  this.reg.escribir_bitacora(this.log, "listar directorio servidor");
								  flujos.enviar_mensaje(listar.listar_directorio(), salida);
								  break;
							  }
							  else if (mensaje.equals("ba")){ // ENVIAR ARCHIVO
								  nombre_archivo_subir = entrada.readLine(); // espera el nombre del archivo
								  this.reg.escribir_bitacora(this.log, "bajando archivo: " + nombre_archivo_subir);
								  System.out.println("enviando archivo...");
							      this.flujos.send(cliente, nombre_archivo_subir, salida);
							      break;
							  }
							  else if (mensaje.equals("sa")){ //RECIBIR ARCHIVO
								  nombre_archivo_bajar = entrada.readLine(); // espera el nombre del archivo
								  reg.escribir_bitacora(this.log, "subiendo archivo: " + nombre_archivo_bajar);
								  System.out.println("bajando archivo...");
							      mensaje = entrada.readLine();
							      archivo_len = Integer.parseInt(mensaje);
							      System.out.println("tamaño archivo: " + mensaje);
							      flujos.receiveFile(cliente, archivo_len, nombre_archivo_bajar);
							      break;
							  }
							  else if (mensaje.equals("fin")){ //RECIBIR ARCHIVO
								  System.out.println("cerrando socket del cliente");
								  finalizar = true;
							      break;    
							  }
							}
						  }
					  } else {
							flujos.enviar_mensaje("Usuario no existente", salida);
							break;
					  	}
				  } else {
						  if(existe.equals("no")){
							  flujos.enviar_mensaje("registrando usuario " + this.log, salida);
							  usuario.Registrar(this.log, this.clave);
						  } else {
							  flujos.enviar_mensaje("Usuario ya existente. Hola " + this.log, salida); // registrando usuario ya existente
						  	}
				  	}
				System.out.println("finalizando");
				this.flujosalida.close();
				this.salida.close();
				this.flujoentrada.close();
				this.entrada.close();
				this.cliente.close();
			} while(true);
		} catch (Exception e){e.printStackTrace();}
		System.out.println("Sale de hilo");	
		Thread.currentThread().interrupt();//preserve the message
		return;
	} // FIN RUN
} // FIN HILO


// CLASE PRINCIPAL //

public class FTP_server {
	static String direccion;
	static ServerSocket socket_servidor;
	Socket socket_cliente;

	public static void main (String[] arg) throws IOException {
			try{			
				direccion = "localhost";
				int puerto = 5972;
				ServerSocket socket_servidor = new ServerSocket(puerto);
				do {
					Socket socket_cliente = socket_servidor.accept();
					new NuevoHilo(socket_cliente);
				}while(true);

			} catch (Exception e) {
				System.out.println("Interrupcion del hilo principal");
		       }
		}
}


// CLASES FLUJOS, BITACORA Y DIRECTORIO

class manejo_flujos{
	
	public void enviar_mensaje(String mensaje, PrintStream salida) {
		salida.println(mensaje);
		salida.flush();
	}
	
	
	@SuppressWarnings("resource")
	public void send(Socket socket_cliente, String archivo, PrintStream salida) throws Exception {
		int archivo_len = 0;
		FileInputStream fis = null;
	    BufferedInputStream bis = null;
	    OutputStream os = null;
		os = socket_cliente.getOutputStream();	
		try {	
		    File myFile = new File("./FTP/Servidor/" + archivo); // ENVIAR ARCHIVO
		    byte[] mybytearray = new byte[(int) myFile.length()];
		    fis = new FileInputStream(myFile);
		    bis = new BufferedInputStream(fis);
		    bis.read(mybytearray, 0, mybytearray.length);
		    os = socket_cliente.getOutputStream();
		    System.out.println("Sending...");
		    System.out.println("Sending " + "./FTP/Servidor/ArchivoServidor.txt" + "(" + mybytearray.length + " bytes)");
		    archivo_len = mybytearray.length;
		    salida.println(archivo_len);
		    salida.flush();
		    os.write(mybytearray, 0, mybytearray.length);
		    os.flush();
	    } finally {
	          if (bis != null) bis.close();
	          if (os != null) os.close();
	        }
	    //os.close();
	    System.out.println("Done.");
	}

	@SuppressWarnings("resource")
	public void receiveFile(Socket socket_cliente, int filesize, String nombre_archivo) throws Exception {
	    int bytesRead;
	    int current = 0;
	    FileOutputStream fos = null;
	    BufferedOutputStream bos = null;
	    byte[] mybytearray = new byte[filesize];
	    InputStream is = socket_cliente.getInputStream();
	    fos = new FileOutputStream("./FTP/Servidor/" + nombre_archivo);
	    bos = new BufferedOutputStream(fos);
	    bytesRead = is.read(mybytearray, 0, mybytearray.length);
	    current = bytesRead;
	    do {
	        bytesRead =
	           is.read(mybytearray, current, (mybytearray.length-current));
	        if(bytesRead >= 0) current += bytesRead;
	    } while(current < filesize);
	    bos.write(mybytearray, 0 , current);
	    bos.flush();
	    //bos.close();
	    System.out.println("File " + "prueba.txt" + " downloaded (" + current + " bytes read)");
	}
}


class bitacora {
	String bitacoranombre;
	public void crear_bitacora(String log){
		Formatter archivo = null;
		try {				
			System.out.println("creando bitacora");
			SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyyy_hhmmss");
			Date curDate = new Date();
			String strDate = sdf.format(curDate);
			bitacoranombre = "./bitacora/" + log + "_bitacora_" + strDate + ".txt";
			archivo = new Formatter(new BufferedWriter(new FileWriter(bitacoranombre, true)));
			}
			catch (IOException ioException){
				ioException.printStackTrace();
			}
			finally{
				if(archivo != null){
					archivo.close();
				}
			}
	}
	
	public void escribir_bitacora(String log, String operacion) {
		Formatter archivo = null;
			try{
				archivo = new Formatter(new BufferedWriter(new FileWriter(bitacoranombre, true)));
				try {
					SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyyy_hhmmss");
					Date curDate = new Date();
					archivo.format("%s %s %s\n", log, operacion, curDate);
				}
				catch (FormatterClosedException formatterClosedException) {
					System.err.println("error escribiendo en el archivo");
					return;
				}
				catch (NoSuchElementException elementException){
					System.err.println("entrada invalida");
				}
			}
			catch (IOException ioException){
				ioException.printStackTrace();
			}
			finally{
				if(archivo != null){
					archivo.close();
					//System.out.println("cerrando...");
				}
			}
	}
} // fin bitacora
				



class directorio_server {
	public String listar_directorio () {
		File folder = new File("./FTP/Servidor");
		File[] listOfFiles = folder.listFiles();
		String str = "";
		for (File file : listOfFiles) {
		    if (file.isFile()) {
		        //System.out.println(file.getName());
		    	str = str + file.getName() + "&";
		    }
		}
		return str;
	}
} // fin directorio_server








