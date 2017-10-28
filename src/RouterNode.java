import java.util.*;
import java.util.Map.Entry;

import javax.swing.plaf.basic.BasicInternalFrameTitlePane.MaximizeAction;

public class RouterNode {
 
	private int myID;
	private GuiTextArea myGUI;
	private RouterSimulator sim;
	private HashMap<Integer, HashMap<Integer, Integer[]>> map;
	private HashMap<Integer, Integer> miEstadoEnlace;
	private Integer serialFlood=1;
	private List<Integer> vecinos;
	private List<Integer> nodosRed;
	private List<RouterPacket> listaFloodControlado=new ArrayList<RouterPacket>();
	//--------------------------------------------------
  
	public RouterNode(int ID, RouterSimulator sim, HashMap<Integer,Integer> costs) {
	    
		myID = ID;
	    this.sim = sim;
	    myGUI =new GuiTextArea("  Output window for Router #"+ ID + "  ");
	    //Instancio map que sera mi tabla de ruteo que contiene En filas el origen, en columnas el destino, y en cada lugar 
	    //el par camino/costo de la forma [Integer camino]integer costo
	    map=  new HashMap<Integer, HashMap<Integer, Integer[]>>();
	    vecinos = new ArrayList<Integer>();
	    nodosRed = new ArrayList<Integer>();
	    
	    //Itero sobre los costos de los vecinos recibido en el construcor
	    Iterator it = costs.entrySet().iterator();
				
		while (it.hasNext()) {
			  
			Map.Entry e = (Map.Entry)it.next();
			//obtengo id del vecino
			Integer vecino=(Integer) e.getKey();
			//obtengo costo del vecino
			Integer vecinoCostoInteger=(Integer) e.getValue();
			//si el vecino no existe lo agrego a mi lista de vecinos, para luego saber a quien notificar
			if (!vecinos.contains(vecino))
				vecinos.add(vecino);
			  
			//si el nodo vecino no existe lo agrego a mi lista de nodos de la red
			if (!nodosRed.contains(vecino))
				nodosRed.add(vecino);
			  
			//Obtengo mi estado de enlace de la tabla de ruteo, sino existe aun lo instancio y me asigno a mi mismo el costo 0
			//miEstadoEnlaceEnTablaR es mis costos  mis vecinos (y a mi mismo), mas los destinos alcanzables y sus costos luego de aplicar dijkstra
			//miEstadoEnlace es solo mis costos a mis vecinos (y a mi mismo)
			HashMap<Integer, Integer[]> miEstadoEnlaceEnTablaR=map.get(myID);
			if(miEstadoEnlaceEnTablaR==null) {
				
				//me agrego a mi mismo como destino
				if (!nodosRed.contains(myID))
					nodosRed.add(myID);
				
				miEstadoEnlaceEnTablaR=new HashMap<Integer,Integer[]>();
				miEstadoEnlaceEnTablaR.put(myID, new Integer[]{myID,0});
			    
				miEstadoEnlace=new HashMap<Integer,Integer>();
				miEstadoEnlace.put(myID, 0);
			    
			}
			 
			//Agrego el costo del vecino correspondiente al paso de la iteracion en este momento
			miEstadoEnlaceEnTablaR.put(vecino, new Integer[]{vecino,vecinoCostoInteger});
			miEstadoEnlace.put(vecino, vecinoCostoInteger);
			//Agrego mi estdoEnlaceEnTabla a mi tabla de ruteo			
			map.put(myID, miEstadoEnlaceEnTablaR);
			
		}
		//relleno los valores infinitos
		rellenarInfinitos();
		   
		//Notifico a todos mis vecinos que hubo cambios dado que antes no tenia datos
		hagoFlooding(null);	
	
	}
  
	private void rellenarInfinitos(){

		for (Integer v1 : nodosRed) {
			for (Integer v2 : nodosRed) {
			
				HashMap<Integer, Integer[]> vecinoVector=map.get(v1);
				if(vecinoVector==null)
					vecinoVector=new HashMap<Integer,Integer[]>();
				if(vecinoVector.get(v2)==null)
					vecinoVector.put(v2, new Integer[]{null,this.sim.INFINITY});
			    map.put(v1, vecinoVector);
			 
			}
		}
	  
	}
  
	private Integer obtenerSerialFlooding(){
		return serialFlood++;
	}
	
	private HashMap<Integer, Integer> obtengoMiEstadoEnlace(){
		
	  //armo de mi tabla de ruteo mi vector de distancia, es decir le quito el componente camino de la calve camino/costo 
	  HashMap<Integer, Integer> dvAEnviar= (HashMap<Integer, Integer>) miEstadoEnlace.clone();
	  dvAEnviar.put(-1, obtenerSerialFlooding());
	  return dvAEnviar;
	  
	}
	
	private void hagoFlooding(RouterPacket pktToFlood){
		
		HashMap<Integer, Integer> dv;
		Integer origen=myID;
		RouterPacket pkt = null;
		
		//si recibo pktToFlood, implica que el paquete me fue enviado por otro nodo, si no recibo es porque este nodo comienza el flooding
		if(pktToFlood==null)
			//si este nodo comienza el flooding obtengo sus estado enlace
			dv= obtengoMiEstadoEnlace();
		else{
			
		  //si es flooding de un paquete de otro nodo obtengo esos datos para el reenvio
		  dv=pktToFlood.mincost;
		  origen=pktToFlood.sourceid;
	  
		}
 
		//recorro la lista de mis vecinos para notificarlos y enviarles mi vector de distancia
		for (Integer vecinoID : vecinos) {
		  
			pkt= new RouterPacket(origen, vecinoID, (HashMap<Integer, Integer>) dv.clone());
			sendUpdate(pkt);
		
		}
		if(pkt!=null)
			listaFloodControlado.add(pkt);
	}
  
	private Boolean floodingControlado(RouterPacket pkt){
	  
		for (RouterPacket pf : listaFloodControlado) 
			if(pkt.sourceid==pf.sourceid && pf.mincost.get(-1)==pkt.mincost.get(-1))
				return true;
		return false;
		
	}
  
	@SuppressWarnings({ "static-access", "unchecked" })
	public void hagoDijkstra(){
	
		//distancias desde el nodo que ejecuta Dijstra a todos los demás nodos.
		//Recordar que Integer[0] es el nodo por donde se debe ir inmediatamente después para llegar desde myID hasta el nodo "columna"
		//Integer[1] es el costo de ir al nodo columna
				
		HashMap<Integer, Integer[]> distancias = this.map.get(this.myID);
		//Uso un hash map para no tener problemas con los indices del array cuando en distancias no tengo todos los nodos
		//o cuando tengo numeros de nodos mayores al tamaño del array.
		//solo se utiliza la pos 0 del array, Integer[0] = 0 indica no visitado, Integer[1] = 1 indica visitado.
		//Dejo el arreglo para poder usar clone.
		HashMap<Integer, Integer[]> visitado = (HashMap<Integer, Integer[]>) distancias.clone();
		
		int cantVisitados = 0;
		
		//Inicializa todos los nodos como no visitados
		Integer[] ini = new Integer[1];
		ini[0] = 0;

		Iterator<Entry<Integer,Integer[]>> it1 = visitado.entrySet().iterator();
		while (it1.hasNext()){
			Map.Entry e = (Map.Entry)it1.next();
		    Integer key=(Integer) e.getKey();
			visitado.put(key, ini);
		}
		Iterator<Entry<Integer,Integer[]>> it2 = distancias.entrySet().iterator();
		while (it2.hasNext()){
			
			Map.Entry<Integer, Integer[]> e = (Map.Entry<Integer, Integer[]>)it2.next();
			if (!this.vecinos.contains(e.getKey()) && e.getKey() != this.myID){
				Integer[] arr = new Integer[2];
				arr[0] = null;
				arr[1] = this.sim.INFINITY;
				distancias.put(e.getKey(), arr);
			} else{
				Integer[] arr = new Integer[2];
				arr[0] = e.getKey();
				arr[1] = this.miEstadoEnlace.get(e.getKey());
				distancias.put(e.getKey(), arr);
			}
						
		}
		
		Integer[] update1 = new Integer[1];
		update1[0] = 1;
		visitado.put(myID, update1);
		cantVisitados ++;
		
		while (cantVisitados != distancias.size()){
			
			//Tomar el nodo mínimo del vector distancia y que no esté visitado;
			int idNodoMin = buscarMinimo(distancias,visitado);
			Integer[] update2 = new Integer[1];
			update2[0] = 1;
			visitado.put(idNodoMin, update2);
			
			cantVisitados ++;
			
			//Las distancias desde el nodo mínimo del vector distancia y que no esté visitado a todos los demás.
			HashMap<Integer, Integer[]> distanciasAlNodoMinimo = this.map.get(idNodoMin);
			//Solo se chequea por los vecinos de ese nodo.
			
			Iterator<Entry<Integer, Integer[]>> it = distancias.entrySet().iterator();
			while (it.hasNext()){
				
				Map.Entry<Integer, Integer[]> aux2 = (Map.Entry<Integer, Integer[]>)it.next();
				
				//Solo si el minimo y el nodo donde está it2 son vecinos.
				if (distanciasAlNodoMinimo.get(aux2.getKey())[1] != this.sim.INFINITY){
				
					int distanciaActual = distancias.get(aux2.getKey())[1];
					int distanciaCandidata = distancias.get(idNodoMin)[1] + distanciasAlNodoMinimo.get(aux2.getKey())[1];
					if (distanciaActual > distanciaCandidata){
						Integer[] arr = new Integer[2];
						//En la pos 0 va el nodo por el que se llega al nodo minimo
						arr[0] = distancias.get(idNodoMin)[0];
						//En la pos 1 va la nueva distancia al nodo donde está it2 
						arr[1] = distanciaCandidata;
						distancias.put(aux2.getKey(), arr);
					}
				
				}
				
			}
			
		}
		
		//Acomodo el vector
		map.put(myID,distancias);
		
	}
	
	private int buscarMinimo(HashMap<Integer, Integer[]> distancias, HashMap<Integer, Integer[]> visitado) {
		
		Iterator<Entry<Integer, Integer[]>> it = distancias.entrySet().iterator();
		
		int min = 2147483647; //Max valor para int 32 bits
		int res = 0;
		
		while (it.hasNext()){
			
			Map.Entry<Integer, Integer[]> e = (Map.Entry<Integer, Integer[]>)it.next();
			if (min > e.getValue()[1] && (visitado.get(e.getKey())[0] == 0)){
				min = e.getValue()[1];
				res = e.getKey();
			}
			
		}
		
		return res;
	}

	//--------------------------------------------------
	public void recvUpdate(RouterPacket pkt){
		
		if(!floodingControlado(pkt)){
			
			HashMap<Integer,Integer> mincost = pkt.mincost;
			//Id del origen del flooding
			Integer origen=pkt.sourceid;
		  
			//si el origen no existe lo agrego a mi lista de nodos de red
			if (!nodosRed.contains(origen)){
				nodosRed.add(origen);
		    	map.put(origen,new HashMap<Integer,Integer[]>());
			}  	
		    
			//itero sobre el estado de enlace de dicho nodo origen
			Iterator it = mincost.entrySet().iterator();
			while (it.hasNext()) {
				
				Map.Entry e = (Map.Entry)it.next();
			    
			    //Obtengo el ID del vecino de dicho origen
			    Integer idVecinoOrigen=(Integer) e.getKey();
			    //Descarto la entrada -1 del hashmap que contiene el serial y no un nodo
			    if(idVecinoOrigen!=-1){
			    	
			    	//Obtengo el costo del vecino de dicho origen
				    Integer costoVecinoOrigen=(Integer) e.getValue();
					
					//Agrego el vecino de dicho origen a mi tabla de ruteo
					map.get(origen).put(idVecinoOrigen,new Integer[]{null,costoVecinoOrigen} );
						    
					//si este vecino del origen no existe lo agrego a mi lista de destinos
					if (!nodosRed.contains(idVecinoOrigen))
						nodosRed.add(idVecinoOrigen);
			    }
			}
		  
			rellenarInfinitos();	  
			hagoFlooding(pkt);
			hagoDijkstra();
		}
	}
  
  //--------------------------------------------------
  private void sendUpdate(RouterPacket pkt){
    sim.toLayer2(pkt);
  }

  private String formatearDato(Integer camino,Integer costo){
	  
	  //formateo los costos e IDs para la salida en pantalla
	  String s;
	  String co;
	  if(costo==sim.INFINITY)
		  co="#";
  	  else
  		  co=costo.toString();
  	
	  String ca;
	  if(camino==null)
		  ca="#";
	  else
		  ca=camino.toString();
  	   
	  ca="["+ca+"]";
	  s=ca+co;
	  s=F.format(s,15);  
	  return s;
  
  }
  
  private String formatearNumero(Integer i){
	  
	  //formateo los costos e IDs para la salida en pantalla
	  String s;
	  if(i==sim.INFINITY)
		s="#";
	  else
		s=i.toString();
	  s=F.format(s, 15);
	  return s;

  }
  
  //--------------------------------------------------
  public void printDistanceTable() {

	  myGUI.println("Current table for " + myID + "  at time " + sim.getClocktime());

	  String cabezal=F.format("O/D" , 15);
	  Boolean cabezalImprimir=true;
	  String out;
	  Iterator itO = map.entrySet().iterator();
	  
	  Boolean origenImprimir;
	  //Itero sobre la tabla de ruteo y mando a pantalla el cabezal y costos
	  while (itO.hasNext()) {
		  Map.Entry o = (Map.Entry)itO.next();
		  Integer y=(Integer) o.getKey();
		  origenImprimir=true;  
		  out="";
		  
		  Iterator itI = ((HashMap<Integer, Integer[]>) o.getValue()).entrySet().iterator();
		  while (itI.hasNext()) {
			  
			  Map.Entry i = (Map.Entry)itI.next();
			  Integer x=(Integer) i.getKey();
			  Integer[] caminoCosto=(Integer[]) i.getValue();
			  if (cabezalImprimir)
				  cabezal=cabezal+formatearNumero(x);				    	
			  if(origenImprimir)
				  out=out+formatearNumero(y);
			  origenImprimir=false;
			  out=out+formatearDato(caminoCosto[0],caminoCosto[1]);
				    
		  }    
			  
		  if(cabezalImprimir)
			  myGUI.println(cabezal);  
		  cabezalImprimir=false;
		  myGUI.println(out);
			  
	  }
  }

  private void rearmoTablaPorLinkUpdate(){
	  
	  HashMap<Integer,Integer[]> miEstadoEnlaceEnTablaR=map.get(myID);
	  Iterator itO = miEstadoEnlace.entrySet().iterator();
	  
	  //Itero sobre mi estado enlace para actualizar la tabla de routeo
	  while (itO.hasNext()) {
		  
		    Map.Entry o = (Map.Entry)itO.next();
		    Integer key=(Integer) o.getKey();
		    Integer value=(Integer) o.getValue();
		    miEstadoEnlaceEnTablaR.put(key, new Integer[]{key,value});
	  
	  }
	  map.put(myID, miEstadoEnlaceEnTablaR);
  }
  
  //--------------------------------------------------
  public void updateLinkCost(int dest, int newcost) {
	  //Me aseguro que el destino sea siempre un nodo vecino y que el costo sea realmente diferente
	  if(vecinos.contains(dest) && map.get(myID).get(dest)[1]!=newcost){
		  
		  //sustituyo el nuevo valor del costo del link
		  miEstadoEnlace.put(dest,newcost);
		  rearmoTablaPorLinkUpdate();
		  hagoFlooding(null);
		  hagoDijkstra();
	  
	  }
  }

}
