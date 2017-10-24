import javax.swing.*;        

import java.util.*;

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
		  if(miEstadoEnlaceEnTablaR==null)
		  {
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
  
  private void rellenarInfinitos()
  {

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
  
  private Integer obtenerSerialFlooding()
  {
	  return serialFlood++;
  }
  private HashMap<Integer, Integer> obtengoMiEstadoEnlace()
  {
	  //armo de mi tabla de ruteo mi vector de distancia, es decir le quito el componente camino de la calve camino/costo 
	  HashMap<Integer, Integer> dvAEnviar= (HashMap<Integer, Integer>) miEstadoEnlace.clone();
	      dvAEnviar.put(-1, obtenerSerialFlooding());
	  return dvAEnviar;
  }
  private void hagoFlooding(RouterPacket pktToFlood)
  {
	  HashMap<Integer, Integer> dv;
	  Integer origen=myID;
	  RouterPacket pkt = null;
	  //si recibo pktToFlood, implica que el paquete me fue enviado por otro nodo, si no recibo es porque este nodo comienza el flooding
	  if(pktToFlood==null)
		  //si este nodo comienza el flooding obtengo sus estado enlace
		  dv= obtengoMiEstadoEnlace();
	  else
	  {
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
  
  private Boolean floodingControlado(RouterPacket pkt)
  {
	  

	  for (RouterPacket pf : listaFloodControlado) 
		  if(pkt.sourceid==pf.sourceid && pf.mincost.get(-1)==pkt.mincost.get(-1))
			  return true;
		
	 
		  return false;
  }
  
  public void hagoDijkstra()
  {
	  
  }
  //--------------------------------------------------
  public void recvUpdate(RouterPacket pkt) {
	 if(!floodingControlado(pkt))
	 {
		  HashMap<Integer,Integer> mincost = pkt.mincost;
		  //Id del origen del flooding
		  Integer origen=pkt.sourceid;
		  
		  //si el origen no existe lo agrego a mi lista de nodos de red
		  if (!nodosRed.contains(origen))
		  {
			  nodosRed.add(origen);
		    	map.put( origen,new HashMap<Integer,Integer[]>());
		  }  	
		    
		  //itero sobre el estado de enlace de dicho nodo origen
		  Iterator it = mincost.entrySet().iterator();
		  while (it.hasNext()) {
			    Map.Entry e = (Map.Entry)it.next();
			    
			    //Obtengo el ID del vecino de dicho origen
			    Integer idVecinoOrigen=(Integer) e.getKey();
			    //Descarto la entrada -1 del hashmap que contiene el serial y no un nodo
			    if(idVecinoOrigen!=-1)
			    {
				    //Obtengo el costo del vecino de dicho origen
				    Integer costoVecinoOrigen=(Integer) e.getValue();
					
					  //Agrego este el vecino de dicho origen a mi tabla de ruteo
					   map.get(origen).put(idVecinoOrigen,new Integer[]{null,costoVecinoOrigen} );
						    
		    	 //si el este vecino del origen no existe lo agrego a mi lista de destinos
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
  private void sendUpdate(RouterPacket pkt) {
    sim.toLayer2(pkt);

  }

  private String formatearDato(Integer camino,Integer costo)
  {
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
private String formatearNumero(Integer i)
{
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
	  myGUI.println("Current table for " + myID +
			"  at time " + sim.getClocktime());

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

  
  private void rearmoTablaPorLinkUpdate()
  {
	  
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
	  if(vecinos.contains(dest) && map.get(myID).get(dest)[1]!=newcost)
	  {
		  //sustituyo el nuevo valor del costo del link
		  miEstadoEnlace.put(dest,newcost);
		  rearmoTablaPorLinkUpdate();
		  hagoFlooding(null);
		  hagoDijkstra();
	  }
  }

}
