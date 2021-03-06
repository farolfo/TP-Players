/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ar.edu.itba.pod.tp.player;

import ar.edu.itba.pod.tp.interfaces.Player;
import ar.edu.itba.pod.tp.interfaces.PlayerDownException;
import ar.edu.itba.pod.tp.interfaces.Referee;
import ar.edu.itba.pod.tp.interfaces.Registration;
import ar.edu.itba.pod.tp.interfaces.Request;
import ar.edu.itba.pod.tp.interfaces.Response;
import ar.edu.itba.pod.tp.interfaces.Utils;

import java.rmi.ConnectException;
import java.rmi.RemoteException;
import java.rmi.UnmarshalException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

/**
 *
 * @author mariano
 */
public class PlayerServer implements Player
{
	private String name;
	private String salt;
	private int id;
	private Integer clientSeq;
	private Integer serverSeq;
	private Referee referee;
	private List<Player> opponents;
	public int total;
	
	public PlayerServer(String name)
	{
		this.name = name;
	}
	
	public String getName(){
		return name;
	}

	public void init(Referee referee) throws RemoteException
	{
		Player playerStub = (Player) UnicastRemoteObject.exportObject(this, 0);
		Registration registration = referee.newPlayer(name, playerStub);
		this.id = registration.id;
		this.clientSeq = registration.clientSeq;
		this.serverSeq = registration.serverSeq;
		this.salt = registration.salt;
		this.referee = referee;
		this.opponents = registration.players;
		this.total = registration.clientTotal;
	}
	
	@Override
	public Response operate(Request request) throws RemoteException
	{
		int myOpSeq;
		synchronized (this.serverSeq) {
			myOpSeq = this.serverSeq++;
		
		Response response = new Response();
		response.reqPlayerId = request.playerId;
		response.reqClientSeq = request.clientSeq;
		response.reqMessage = request.message;
		response.reqHash = request.hash;

		response.rspPlayerId = this.id;
		response.rspServerSeq = myOpSeq;
		response.rspMessage = request.message + "aaaa";
		response.rspHash = hashMessage(myOpSeq, response.rspMessage);
		
		this.referee.registerResponse(this, response);
		PlayerApp.asServer.incrementAndGet();
		return response;
		}
	}

	public void play(String message, Player target) throws RemoteException, InterruptedException
	{
		Request request = this.buildAndRegisterRequest(message);
		try {
			Response response = target.operate(request);
			System.out.println("result " + response);
		}
		catch (ConnectException e) {
			e.printStackTrace();
			throw new PlayerDownException(e.getMessage(), e);
		}
		catch (UnmarshalException e) {
			e.printStackTrace();
			throw new PlayerDownException(e.getMessage(), e);
		}
		catch (Exception e) {
			System.out.println("\nAca !\n");
			e.printStackTrace();
			throw new PlayerDownException(e.getMessage(), e);
		}
//		Thread.sleep(100);
	}
	
	public List<Player> getOpponents()
	{
		return this.opponents;
	}
	
	private String hashMessage(int opSeq, String message)
	{
		return Utils.hashMessage(this.id, opSeq, message, this.salt);
	}
	
	/* Synchronized agregado a este metodo 
	 * */
	private Request buildAndRegisterRequest(String message) throws RemoteException, InterruptedException {
		synchronized (this.clientSeq) {
			int myOpSeq = this.clientSeq++;
			Request request = new Request(this.id, myOpSeq, message, hashMessage(myOpSeq, message));
	
			System.out.println("invoke " + request);
			this.referee.registerRequest(this, request);
			return request;
		}
	}
	
	public Boolean isFinnished() {
		return PlayerApp.asClient.get() > PlayerApp.loop && PlayerApp.asServer.get() > PlayerApp.loop;
	}
	
	public String results() {
		return "Server : " + PlayerApp.asServer.get() + " - Client : " + PlayerApp.asClient.get();
	}
}
