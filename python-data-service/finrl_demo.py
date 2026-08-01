import numpy as np, pandas as pd
from finrl.meta.env_stock_trading.env_stocktrading import StockTradingEnv
from finrl.agents.stablebaselines3.models import DRLAgent

print('1. 准备数据...')
codes = ['000001','600519']
num_days = 504
dates = pd.date_range('2024-01-01', periods=num_days, freq='B')
prices_map = {}
for c in codes:
    base = 50.0 if c == '000001' else 150.0
    p = [base]
    for _ in range(num_days-1):
        p.append(p[-1]*(1+np.random.normal(0,0.02)))
    prices_map[c] = p

all_data = []
for day_idx, d in enumerate(dates):
    for c in codes:
        cp = prices_map[c][day_idx]
        all_data.append({'date':d,'tic':c,'open':cp*0.99,'high':cp*1.02,'low':cp*0.98,'close':cp,'volume':np.random.randint(1000000,5000000)})
df = pd.DataFrame(all_data)
df = df.sort_values(['date','tic']).reset_index(drop=True)
df.index = [i // len(codes) for i in range(len(df))]
tech_list = ['open','high','low','close','volume']
print(f'   数据: {df.shape}, 天数: {len(dates)}')

print('2. 创建环境...')
state_space = 1 + 2*len(codes) + len(codes)*len(tech_list)
action_space = len(codes)
kw = {'df':df, 'stock_dim':len(codes), 'hmax':100, 'initial_amount':100000,
      'num_stock_shares':[0]*len(codes), 'buy_cost_pct':[0.001]*len(codes),
      'sell_cost_pct':[0.001]*len(codes), 'reward_scaling':1e-4,
      'state_space':state_space, 'action_space':action_space,
      'tech_indicator_list':tech_list, 'make_plots':False, 'print_verbosity':10}
env = StockTradingEnv(**kw)

print('3. 训练PPO (5000步, 约1分钟)...')
agent = DRLAgent(env=env)
model = agent.get_model('ppo', model_kwargs={'n_steps':128,'learning_rate':0.00025,'batch_size':64})
trained = agent.train_model(model=model, tb_log_name='demo', total_timesteps=5000)

print('4. 回测...')
env2 = StockTradingEnv(**kw)
obs,_ = env2.reset()
total = 0; done = False; steps = 0
while not done:
    a,_ = trained.predict(obs, deterministic=True)
    obs,r,done,tr,_ = env2.step(a)
    total += r; steps += 1
print(f'   步数:{steps}  收益:{total:.2f}  最终资产:{env2.initial_amount+total:,.2f}')
print('>>> FinRL 演示完成!')
