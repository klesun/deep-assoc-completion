
var makeCustomer = () => {
    return {
        name: 'Vasya',
        money: '1500.00',
        loyalty: 'low',
        friends: [makeCustomer(), makeCustomer()],
        sales: [
            {dt: '2017-05-12', product: 'candies', price: '15.00'},
            {dt: '2017-09-12', product: 'paint', price: '45.00'},
            {dt: '2017-11-12', product: 'Jack Daniels', price: '70.00'},
        ],
    }
};