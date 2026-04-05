print("=== 测试2: 调用Java对象 ===")
print("检查 emu: " .. tostring(emu))

if emu == nil then
    print("错误: emu 为 nil")
    return "test2 FAILED: emu is nil"
end

print("emu 类型: " .. type(emu))

-- 测试调用无阻塞方法
print("调用 emu:back()...")
local ok = emu:back()
print("back() 返回: " .. tostring(ok))

print("测试2通过")
return "test2 OK"
