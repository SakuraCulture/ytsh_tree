export function shouldLoadUnreadMessages({
  hasAccessToken,
  isUserReady
}: {
  hasAccessToken: boolean
  isUserReady: boolean
}) {
  return hasAccessToken && isUserReady
}
