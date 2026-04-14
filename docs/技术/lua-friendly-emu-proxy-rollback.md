# Lua-Friendly Emu Proxy Rollback

If Lua tool execution becomes unstable after enabling `LuaFriendlyEmuFacadeProxy`, rollback with the steps below.

## 1) Switch Lua injection back to raw facade

In `PhoneEmulationTool`, replace:

- `AppContainer.getInstance().luaFriendlyEmuFacadeProxy`

with:

- `AppContainer.getInstance().emuFacade`

This restores the previous script binding behavior immediately.

## 2) Keep proxy classes in place

Do not delete `LuaFriendlyEmuFacadeProxy` or `LuaValueConverter` during rollback. Keeping the code allows fast re-enable and targeted fixes.

## 3) Verify rollback

Run Lua regression scripts (app open, app listing, UI query) and confirm behavior matches the pre-proxy baseline.

## 4) Re-enable plan

When ready to retry:

1. Fix converter/proxy issues.
2. Re-run unit and regression tests.
3. Switch the injection line back to `luaFriendlyEmuFacadeProxy`.
