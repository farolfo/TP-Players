package ar.edu.itba.pod.tp.player;

import java.rmi.RemoteException;
import java.util.List;

import ar.edu.itba.pod.tp.interfaces.Player;
import ar.edu.itba.pod.tp.interfaces.PlayerDownException;
 
public class PlayThread implements Runnable{

	PlayerServer server;
	
	PlayThread(PlayerServer server) {
		this.server = server;
	}
	
	@Override
	public void run() {
		List<Player> players = server.getOpponents();
		do {
			int opt = (int) (java.lang.Math.random() * players.size());
			Player other = players.get(opt);
			
			synchronized (PlayerApp.plays) {
				if( other != null && PlayerApp.plays < PlayerApp.loop) {
					synchronized (other) {
						if ( players.contains(other) && PlayerApp.plays < PlayerApp.loop){
							try {	
								server.play("hola! estamos jugando " + PlayerApp.plays, other);
								++PlayerApp.plays;
							}
							catch (PlayerDownException e) {
								System.out.println("ERR  >> Fallo la jugada, se remueve el jugador");
								players.remove(opt);
							} catch (InterruptedException e) {
								System.out.println("ERR  >> Fallo la jugada");
								// TODO Auto-generated catch block
								e.printStackTrace();
							} catch (RemoteException e) {
								e.printStackTrace();
								System.err.println("ERR  >> Client exception: " + e.toString());			
								System.err.println("PERDI!");
								System.exit(1);
							}
						}
					}
				}
			}
		} while (PlayerApp.plays < PlayerApp.loop);
	}
}
