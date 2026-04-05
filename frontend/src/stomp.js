import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client/dist/sockjs'

export function createStompClient(onConnect) {
  const client = new Client({
    webSocketFactory: () => new SockJS('/ws'),
    reconnectDelay: 5000,
    heartbeatIncoming: 4000,
    heartbeatOutgoing: 4000,
    onConnect: () => {
      if (onConnect) onConnect(client)
    },
    onStompError: (frame) => {
      console.error('STOMP error:', frame.headers['message'])
    },
  })

  client.activate()
  return client
}
