import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

const WS_URL = import.meta.env.VITE_WS_URL || 'http://localhost:8080/ws'

let stompClient: Client | null = null

export function connectWebSocket(
  ticketId: number,
  onMessage: (msg: any) => void
): Client {
  const token = sessionStorage.getItem('token')
  const client = new Client({
    webSocketFactory: () => new SockJS(WS_URL),
    connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
    debug: () => {},
    reconnectDelay: 5000,
    onConnect: () => {
      client.subscribe(`/topic/tickets/${ticketId}`, (message) => {
        onMessage(JSON.parse(message.body))
      })
    },
  })

  client.activate()
  stompClient = client
  return client
}

export function disconnectWebSocket(client: Client) {
  client.deactivate()
}

export function subscribeToNotifications(
  onNotification: (notif: any) => void
): Client {
  const token = sessionStorage.getItem('token')
  const client = new Client({
    webSocketFactory: () => new SockJS(WS_URL),
    connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
    debug: () => {},
    reconnectDelay: 5000,
    onConnect: () => {
      client.subscribe('/user/queue/notifications', (message) => {
        onNotification(JSON.parse(message.body))
      })
    },
  })

  client.activate()
  return client
}
