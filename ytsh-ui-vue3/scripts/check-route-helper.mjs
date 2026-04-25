import assert from 'node:assert/strict'
import { createServer } from 'vite'

const server = await createServer({
  configFile: 'vite.config.ts',
  server: { middlewareMode: true },
  logLevel: 'error'
})

try {
  const { generateRoute } = await server.ssrLoadModule('/src/utils/routerHelper.ts')
  const [route] = generateRoute([
    {
      id: 1,
      parentId: 0,
      name: '门店商品同步',
      path: 'store-goods-sync',
      component: 'ele/storeGoodsSync/index',
      componentName: 'StoreGoodsSync',
      icon: 'ep:goods',
      visible: true,
      keepAlive: true,
      alwaysShow: true
    }
  ])

  assert.equal(route.path, '/store-goods-sync')
  assert.equal(route.children?.[0]?.path, '')

  const [eleRoute] = generateRoute([
    {
      id: 2,
      parentId: 0,
      name: '门店商品查询与同步',
      path: '/ele/store-goods',
      component: 'ele/store-goods/index',
      componentName: 'EleStoreGoodsSync',
      icon: 'ep:goods',
      visible: true,
      keepAlive: true,
      alwaysShow: true
    }
  ])

  assert.equal(typeof eleRoute.children?.[0]?.component, 'function')
} finally {
  await server.close()
}
