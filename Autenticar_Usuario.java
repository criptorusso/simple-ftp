package autenticacion;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Formatter;
import java.util.FormatterClosedException;
import java.util.NoSuchElementException;

public class Autenticar_Usuario {
	String exito;
	public Autenticar_Usuario(String log, String clave){
		exito = "no";
	}

// METODO PARA AUTENTICAR USUARIO EXISTENTE
	public String Autenticar(String log, String clave) {
		BufferedReader archivo = null;
		try{
			String lectura_linea;
			archivo = new BufferedReader(new FileReader("autenticar.txt"));
			while ((lectura_linea = archivo.readLine()) != null) {
				String[] palabra = lectura_linea.split(" ");
				if(log.equals(palabra[0]) && clave.equals(palabra[1])){
					exito = "si";
					break;
				} 				
			}
			if (exito=="no"){
				System.out.println("usuario " + log + " no existente");
				exito = "no";
			} else {
				System.out.println("hola " + log);
				}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (archivo != null)archivo.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
			}
		System.out.println(exito);
		return exito;
	}

// METODO REGISTRAR USUARIO INEXISTENTE
	public void Registrar(String log, String clave) {
		Formatter archivo = null;
		try {				
				archivo = new Formatter(new BufferedWriter(new FileWriter("autenticar.txt", true)));
				try{
						archivo.format("%s %s\n", log, clave);
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
					System.out.println("cerrando...");
				}
			}

		}
	}


