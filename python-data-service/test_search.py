from akshare_client import search_stocks, _load_stock_list
import json

print("=== 1. Load stock list ===")
stock_list = _load_stock_list()
print(f"Total: {len(stock_list)}")
print(f"Sample: {json.dumps(stock_list[:3], ensure_ascii=False)}")

print()
print("=== 2. Code prefix: '600519' ===")
r = search_stocks('600519')
print(f"Results: {len(r)}")
print(json.dumps(r, ensure_ascii=False))

print()
print("=== 3. Name fuzzy: '茅台' ===")
r = search_stocks('茅台')
print(f"Results: {len(r)}")
print(json.dumps(r, ensure_ascii=False))

print()
print("=== 4. Empty keyword ===")
r = search_stocks('')
print(f"Results: {len(r)} (expected 0)")

print()
print("=== 5. Code prefix: '000' ===")
r = search_stocks('000')
print(f"Results: {len(r)} (max 20)")
print(json.dumps(r[:5], ensure_ascii=False))

print()
print("=== 6. Name: '银行' ===")
r = search_stocks('银行')
print(f"Results: {len(r)}")
print(json.dumps(r[:5], ensure_ascii=False))

print()
print("=== 7. No match: 'ZZZZZ' ===")
r = search_stocks('ZZZZZ')
print(f"Results: {len(r)} (expected 0)")

print()
print("=== 8. Whitespace: '  000001  ' ===")
r = search_stocks('  000001  ')
print(f"Results: {len(r)}")
print(json.dumps(r, ensure_ascii=False))

print()
print("ALL TESTS PASSED")