import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class RouterNode {
 
	private int myID;
	private GuiTextArea myGUI;
	private RouterSimulator sim;
	private HashMap<Integer, HashMap<Integer, Integer[]>> map;
	private HashMap<Integer, Integer> miEstadoEnlace;
	private Integer serialFlood=1;

	private List<RouterPacket> listaFloodControlado=new ArrayList<RouterPacket>();
	//--------------------------------------------------
  
	public RouterNode(int ID, RouterSimulator sim, HashMap<Integer, Integer> costs) {
	    
		myID = ID;
	    this.sim = sim;
	    myGUI =new GuiTextArea("  Output window for Router #"+ ID + "  ");
	    //Instancio map que sera mi tabla de ruteo que contiene En filas el origen, en columnas el destino, y en cada lugar 
	    //el par camino/costo de la forma [Integer camino]integer costo
	    map=  new HashMap<Integer, HashMap<Integer, Integer[]>>();
	   
	    //Itero sobre los costos de los vecinos recibido en el construcor
	    Iterator<Entry<Integer, Integer>> it = costs.entrySet().iterator();
				
		while (it.hasNext()) {
			  
			Map.Entry<Integer, Integer> e = (Map.Entry<Integer, Integer>)it.next();
			//obtengo id del vecino
			Integer vecino=(Integer) e.getKey();
			//obtengo costo del vecino
			Integer vecinoCostoInteger=(Integer) e.getValue();
			
			//Obtengo mi estado de enlace de la tabla de ruteo, sino existe aun lo instancio y me asigno a mi mismo el costo 0
			//miEstadoEnlaceEnTablaR es mis costos  mis vecinos (y a mi mismo), mas los destinos alcanzables y sus costos luego de aplicar dijkstra
			//miEstadoEnlace es solo mis costos a mis vecinos (y a mi mismo)
			HashMap<Integer, Integer[]> miEstadoEnlaceEnTablaR=map.get(myID);
			
			if(miEstadoEnlaceEnTablaR==null) {
				
				//me agrego a mi mismo como destino
				miEstadoEnlaceEnTablaR=new HashMap<Integer,Integer[]>();
				miEstadoEnlaceEnTablaR.put(myID, new Integer[]{myID,0,0});
				miEstadoEnlace=new HashMap<Integer,Integer>();
				//miEstadoEnlace.put(myID, 0);
			    
			}
			 
			//Agrego el costo del vecino correspondiente al paso de la iteracion en este momento
			miEstadoEnlaceEnTablaR.put(vecino, new Integer[]{vecino,vecinoCostoInteger,0});
			miEstadoEnlace.put(vecino, vecinoCostoInteger);
			//Agrego mi estdoEnlaceEnTabla a mi tabla de ruteo			
			map.put(myID, miEstadoEnlaceEnTablaR);
			
		}
		//relleno los valores infinitos y aprendo topologia
		aprendoTopologia(myID);
		   
		//Notifico a todos mis vecinos que hubo cambios dado que antes no tenia datos
		hagoFlooding(null);	
	
	}
  
	@SuppressWarnings("static-access")
	private void aprendoTopologia(Integer idNodoConNuevoVecino){

		//hago la matriz cuadrada con lo nuevo, e infinitos donde corresponda
		HashMap<Integer, Integer[]> nodosRed=map.get(idNodoConNuevoVecino);
		if(nodosRed!=null){
			
			Iterator<Entry<Integer,Integer[]>> it1 = nodosRed.entrySet().iterator();
			while (it1.hasNext()){
				
				Map.Entry<Integer, Integer[]> e = (Map.Entry<Integer, Integer[]>)it1.next();
				Integer v1=e.getKey();
	
				Iterator<Entry<Integer,Integer[]>> it2 = nodosRed.entrySet().iterator();
				while (it2.hasNext()){
					
					Map.Entry<Integer, Integer[]> e2 = (Map.Entry<Integer, Integer[]>)it2.next();
					Integer v2=e2.getKey();
				
								HashMap<Integer, Integer[]> vecinoVector=map.get(v1);
								if(vecinoVector==null)
									vecinoVector=new HashMap<Integer,Integer[]>();
								if(vecinoVector.get(v2)==null)
								{
	
									if(v1==v2)
										vecinoVector.put(v2, new Integer[]{null,0,0});
										else
									vecinoVector.put(v2, new Integer[]{null,this.sim.INFINITY,0});
								}
							    map.put(v1, vecinoVector);
				 
				}
			}	
		} 
	}
  
	private Integer obtenerSerialFlooding(){
		return serialFlood++;
	}
	
	@SuppressWarnings("unchecked")
	private HashMap<Integer, Integer> obtengoMiEstadoEnlace(){
		
		//armo de mi tabla de ruteo mi vector de distancia, es decir le quito el componente camino de la calve camino/costo 
		HashMap<Integer, Integer> dvAEnviar=null;
		if(miEstadoEnlace!=null){
			
			 dvAEnviar= (HashMap<Integer, Integer>) miEstadoEnlace.clone();
			 //pongo en la posición -1 el serial del Flooding
			 dvAEnviar.put(-1, obtenerSerialFlooding());
		
		}
		return dvAEnviar;
	  
	}
	
	@SuppressWarnings("unchecked")
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
		if(dv!=null){
				
			//recorro la lista de mis vecinos para notificarlos y enviarles mi vector de distancia
			HashMap<Integer, Integer> vecinos=miEstadoEnlace;
			Iterator<Entry<Integer,Integer>> it1 = vecinos.entrySet().iterator();
			while (it1.hasNext()){
				Integer vecinoID=((Map.Entry<Integer, Integer>)it1.next()).getKey();
				pkt= new RouterPacket(origen, vecinoID, (HashMap<Integer, Integer>) dv.clone());
				sendUpdate(pkt);
			}
			if(pkt!=null)
				listaFloodControlado.add(pkt);
		
		}
	}
  
	private Boolean floodingControlado(RouterPacket pkt){
	  
		for (RouterPacket pf : listaFloodControlado) 
			if(pkt.sourceid==pf.sourceid && pf.mincost.get(-1)==pkt.mincost.get(-1))
				return true;
		return false;
		
	}
  
	@SuppressWarnings("static-access")
	public void hagoDijkstra(){
	
		//miEstadoEnlaceEnTablaR es la fila de su propio map para el nodo que ejecuta Dijkstra.
		//Recordar que Integer[0] es el nodo por donde se debe ir inmediatamente después para llegar desde myID hasta el nodo "columna"
		//Integer[1] es el costo de ir al nodo columna
		//Integer[2] indica con 0 si el nodo no fue visitado por Dijkstra, con 1 si ya lo fue
				
		HashMap<Integer, Integer[]> miEstadoEnlaceEnTablaR = this.map.get(this.myID);
		
		int cantVisitados = 0;
		
		miEstadoEnlaceEnTablaR.get(myID)[2]=1;
		cantVisitados ++;
		
		while (cantVisitados <= miEstadoEnlaceEnTablaR.size()){
			
			//Tomar el nodo mínimo del vector distancia y que no esté visitado;
			//int idNodoMin = buscarMinimo(distancias,visitado);
			Integer idNodoMin = buscarMinimo(miEstadoEnlaceEnTablaR);
			if(idNodoMin==null)
				cantVisitados=miEstadoEnlaceEnTablaR.size()+1;
			else{
				
				miEstadoEnlaceEnTablaR.get(idNodoMin)[2]=1;
				cantVisitados ++;
				
				//Las distancias desde el nodo mínimo del vector distancia y que no esté visitado a todos los demás.
				HashMap<Integer, Integer[]> distanciasAlNodoMinimo = this.map.get(idNodoMin);
				//Solo se chequea por los vecinos de ese nodo.
				
				Iterator<Entry<Integer, Integer[]>> it = miEstadoEnlaceEnTablaR.entrySet().iterator();
				while (it.hasNext()){
					
					Map.Entry<Integer, Integer[]> aux2 = (Map.Entry<Integer, Integer[]>)it.next();
					
					//Solo si el minimo y el nodo donde está it2 son vecinos. (Recordar que la fila del map para el nodo minimo no tiene los calculos de Dijkstra para ese nodo
					//sino que tiene la topología de la red para el mismo
					if (distanciasAlNodoMinimo.get(aux2.getKey())[1] != this.sim.INFINITY){
					
						int distanciaActual = miEstadoEnlaceEnTablaR.get(aux2.getKey())[1];
						int distanciaCandidata = miEstadoEnlaceEnTablaR.get(idNodoMin)[1] + distanciasAlNodoMinimo.get(aux2.getKey())[1];
						if (distanciaActual > distanciaCandidata){
							
							//En la pos 0 va el nodo por el que se llega al nodo minimo
							//En la pos 1 va la nueva distancia al nodo donde está it2 
							//Se copia en el nodo la puerta por donde sale el padre.
							miEstadoEnlaceEnTablaR.put( aux2.getKey(),new Integer[]{miEstadoEnlaceEnTablaR.get(idNodoMin)[0],distanciaCandidata,miEstadoEnlaceEnTablaR.get( aux2.getKey())[2]});
						
						}
					
					}
				}
			}		
		}
		
		//Acomodo el vector
		map.put(myID,miEstadoEnlaceEnTablaR);
		
	}
	
	@SuppressWarnings("static-access")
	private Integer buscarMinimo(HashMap<Integer, Integer[]> distancias) {
		
		//Devuelve el id del nodo al cual es posible ir en menor costo y que aún no fue visitado por Dijkstra
				
		Iterator<Entry<Integer, Integer[]>> it = distancias.entrySet().iterator();
		
		int min = sim.INFINITY; 
		Boolean primero=true;
		Integer res =null;
		
		while (it.hasNext()){
			
			Map.Entry<Integer, Integer[]> e = (Map.Entry<Integer, Integer[]>)it.next();
			if((e.getValue()[2] == 0))
			{
				if(primero)
				{
					res = e.getKey();
					primero=false;
				}
				if ((min > e.getValue()[1] ) ){
					min = e.getValue()[1];
					res = e.getKey();
					
				}
			}
			
		}
		
		return res;
		
	}

	public void recvUpdate(RouterPacket pkt){
		
		if(!floodingControlado(pkt)){
			
			HashMap<Integer,Integer> mincost = pkt.mincost;
			//Id del origen del flooding
			Integer origen=pkt.sourceid;
		  
			//si el origen no existe lo agrego a mi lista de nodos de red
			if (map.get(origen)==null){
				
				map.put(origen,new HashMap<Integer,Integer[]>());
				map.get(origen).put(origen,new Integer[]{null,0,0} );
				
			}  	
		    
			//itero sobre el estado de enlace de dicho nodo origen
			Iterator<Entry<Integer, Integer>> it = mincost.entrySet().iterator();
			while (it.hasNext()) {
				
				Map.Entry<Integer,Integer> e = (Map.Entry<Integer,Integer>)it.next();
			    
			    //Obtengo el ID del vecino de dicho origen
			    Integer idVecinoOrigen=(Integer) e.getKey();
			    //Descarto la entrada -1 del hashmap que contiene el serial y no un nodo
			    if(idVecinoOrigen!=-1){
			    	
			    	//Obtengo el costo del vecino de dicho origen
				    Integer costoVecinoOrigen=(Integer) e.getValue();
					
					//Agrego el vecino de dicho origen a mi tabla de ruteo
					map.get(origen).put(idVecinoOrigen,new Integer[]{null,costoVecinoOrigen,0} );
						    
					//si este vecino del origen no existe lo agrego a mi lista de destinos
			    
			    }			    
			
			}
			
			//relleno los valores infinitos y aprendo topologia
			aprendoTopologia(origen);	  
			rearmoTabla();
			hagoFlooding(pkt);
			hagoDijkstra();
			
		}
	}
  
	private void sendUpdate(RouterPacket pkt){
		sim.toLayer2(pkt);
	}

	@SuppressWarnings("static-access")
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
  
	@SuppressWarnings("static-access")
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
  
	public void printDistanceTable() {

		myGUI.println("Current table for " + myID + "  at time " + sim.getClocktime());

		String cabezal=F.format("O/D" , 15);
		Boolean cabezalImprimir=true;
		String out;
		Iterator<Entry<Integer, HashMap<Integer, Integer[]>>> itO = map.entrySet().iterator();
	  
		Boolean origenImprimir;
		//Itero sobre la tabla de ruteo y mando a pantalla el cabezal y costos
		while (itO.hasNext()) {
			Map.Entry<Integer, HashMap<Integer, Integer[]>> o = (Map.Entry<Integer, HashMap<Integer, Integer[]>>)itO.next();
			Integer y=(Integer) o.getKey();
			origenImprimir=true;  
			out="";
		  
			Iterator<Entry<Integer, Integer[]>> itI = ((HashMap<Integer, Integer[]>) o.getValue()).entrySet().iterator();
			while (itI.hasNext()) {
			  
				Map.Entry<Integer, Integer[]> i = (Map.Entry<Integer, Integer[]>)itI.next();
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

	@SuppressWarnings("static-access")
	private void rearmoTabla(){
	  
		HashMap<Integer,Integer[]> miEstadoEnlaceEnTablaR=map.get(myID);
	  
		Iterator<Entry<Integer, Integer[]>> itO = miEstadoEnlaceEnTablaR.entrySet().iterator();
		miEstadoEnlace.entrySet().iterator();
	  
		//Itero sobre mi estado enlace para actualizar la tabla de routeo
		while (itO.hasNext()) {
		  
			Map.Entry<Integer, Integer[]> o = (Map.Entry<Integer, Integer[]>)itO.next();
		    Integer key=(Integer) o.getKey();
		    
		    if(miEstadoEnlace.containsKey(key))
		   		miEstadoEnlaceEnTablaR.put(key, new Integer[]{key,miEstadoEnlace.get(key),0});
		    else
		        miEstadoEnlaceEnTablaR.put(key, new Integer[]{null,sim.INFINITY,0});
		   
		}
		miEstadoEnlaceEnTablaR.put(myID, new Integer[]{myID,0,0});
		map.put(myID, miEstadoEnlaceEnTablaR);
	  
	}
  
	public void updateLinkCost(int dest, int newcost) {
		
		//Me aseguro que el destino sea siempre un nodo vecino y que el costo sea realmente diferente
		if(miEstadoEnlace.containsKey(dest) && map.get(myID).get(dest)[1]!=newcost){
		  
			//sustituyo el nuevo valor del costo del link
			miEstadoEnlace.put(dest,newcost);
		  
			rearmoTabla();
			hagoFlooding(null);
			hagoDijkstra();
	  
		}
	}

}
